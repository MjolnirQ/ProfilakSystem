package ru.nsu.resortbooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "PROFILAK_STAFF_EMAIL=staff@example.test",
        "PROFILAK_STAFF_PASSWORD=staff-test-password"
})
@AutoConfigureMockMvc
@Import(BookingFlowTest.Database.class)
class BookingFlowTest {
    @TempDir
    static Path uploads;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("file.storage.location", () -> uploads.toString());
    }

    @Test
    void bookingFlowRespectsRolesCapacityAndOwnership() throws Exception {
        MockHttpSession staff = login("staff@example.test", "staff-test-password", "ROLE_STAFF");
        MockHttpSession first = registerStudent("first@g.nsu.ru");
        MockHttpSession second = registerStudent("second@g.nsu.ru");

        long stay = id(mvc.perform(post("/staff/sessions").session(staff).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"startDate":"2027-01-01","endDate":"2027-01-08","capacity":1}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        long firstBooking = submit(first, stay);
        long secondBooking = submit(second, stay);
        mvc.perform(multipart("/student/requests").file(document()).param("sessionId", Long.toString(stay))
                        .session(first).with(csrf()))
                .andExpect(status().isConflict());

        String documents = mvc.perform(get("/staff/requests/{id}/documents", firstBooking).session(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filePath").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long documentId = json.readTree(documents).get(0).get("id").asLong();
        mvc.perform(get("/staff/documents/{id}", documentId).session(first))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/documents/{id}", documentId).session(staff))
                .andExpect(status().isOk());

        int firstStatus;
        int secondStatus;
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var one = executor.submit(approve(firstBooking, staff, start));
            var two = executor.submit(approve(secondBooking, staff, start));
            start.countDown();
            firstStatus = one.get(10, TimeUnit.SECONDS);
            secondStatus = two.get(10, TimeUnit.SECONDS);
        }
        assertThat(new int[]{firstStatus, secondStatus}).containsExactlyInAnyOrder(200, 409);
        long approved = firstStatus == 200 ? firstBooking : secondBooking;
        MockHttpSession owner = firstStatus == 200 ? first : second;
        MockHttpSession other = firstStatus == 200 ? second : first;

        mvc.perform(get("/staff/sessions").session(staff))
                .andExpect(jsonPath("$[0].bookedCount").value(1));
        mvc.perform(post("/staff/requests/{id}/approve", approved).session(staff).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(post("/student/requests/{id}/pay", approved).session(other).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/student/requests/{id}/pay", approved).session(owner))
                .andExpect(status().isForbidden());
        mvc.perform(post("/student/requests/{id}/pay", approved).session(owner).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPaid").value(true));
        mvc.perform(post("/auth/logout").session(owner).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private Callable<Integer> approve(long id, MockHttpSession staff, CountDownLatch start) {
        return () -> {
            start.await();
            return mvc.perform(post("/staff/requests/{id}/approve", id).session(staff).with(csrf()))
                    .andReturn().getResponse().getStatus();
        };
    }

    private MockHttpSession registerStudent(String email) throws Exception {
        mvc.perform(post("/auth/register").with(csrf()).contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(new Registration(email, "student-test-password", "STAFF"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
        return login(email, "student-test-password", "ROLE_STUDENT");
    }

    private MockHttpSession login(String email, String password, String role) throws Exception {
        return (MockHttpSession) mvc.perform(post("/auth/login").with(csrf()).contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role))
                .andReturn().getRequest().getSession(false);
    }

    private long submit(MockHttpSession student, long stay) throws Exception {
        return id(mvc.perform(multipart("/student/requests").file(document())
                        .param("sessionId", Long.toString(stay))
                        .param("comment", "Test comment")
                        .session(student).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.comment").value("Test comment"))
                .andReturn().getResponse().getContentAsString());
    }

    private long id(String response) throws Exception {
        return json.readTree(response).get("id").asLong();
    }

    private MockMultipartFile document() {
        return new MockMultipartFile("files", "test.txt", "text/plain", new byte[]{1, 2, 3});
    }

    private record Login(String email, String password) {
    }

    private record Registration(String email, String password, String role) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Database {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16");
        }
    }
}
