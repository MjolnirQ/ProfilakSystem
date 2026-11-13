package ru.nsu.resortbooking.adapter.persistence;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.nsu.resortbooking.application.port.BookingStore;
import ru.nsu.resortbooking.domain.Booking;
import ru.nsu.resortbooking.domain.Stay;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBookingStore implements BookingStore {
    private static final String VIEW = """
            select b.*, u.email, s.start_date, s.end_date, s.capacity, s.booked_count
            from booking_requests b
            join users u on u.id = b.user_id
            join sessions s on s.id = b.session_id
            """;
    private static final RowMapper<BookingView> VIEW_MAPPER = (rs, row) ->
            new BookingView(toBooking(rs), rs.getString("email"), toStay(rs, "session_id"));
    private static final RowMapper<Attachment> ATTACHMENT_MAPPER = (rs, row) ->
            new Attachment(rs.getLong("id"), rs.getString("file_name"), rs.getString("file_path"));

    private final JdbcClient jdbc;

    public JdbcBookingStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Stay> listStays() {
        return jdbc.sql("select * from sessions order by start_date, id")
                .query((rs, row) -> toStay(rs, "id"))
                .list();
    }

    @Override
    public Optional<Stay> findStay(long id) {
        return readStay(id, "select * from sessions where id = :id");
    }

    @Override
    public Optional<Stay> lockStay(long id) {
        return readStay(id, "select * from sessions where id = :id for update");
    }

    private Optional<Stay> readStay(long id, String query) {
        return jdbc.sql(query)
                .param("id", id)
                .query((rs, row) -> toStay(rs, "id"))
                .optional();
    }

    @Override
    public Stay saveStay(Stay stay) {
        if (stay.id() != 0) {
            jdbc.sql("update sessions set booked_count = :count where id = :id")
                    .param("count", stay.bookedCount())
                    .param("id", stay.id())
                    .update();
            return stay;
        }
        long id = jdbc.sql("""
                insert into sessions(start_date, end_date, capacity, booked_count)
                values (:start, :end, :capacity, 0) returning id
                """)
                .param("start", stay.startDate())
                .param("end", stay.endDate())
                .param("capacity", stay.capacity())
                .query(Long.class)
                .single();
        return new Stay(id, stay.startDate(), stay.endDate(), stay.capacity(), 0);
    }

    @Override
    public boolean exists(long studentId, long stayId) {
        return jdbc.sql("""
                select exists(select 1 from booking_requests where user_id = :student and session_id = :stay)
                """)
                .param("student", studentId)
                .param("stay", stayId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public Optional<Booking> lockBooking(long id) {
        return jdbc.sql("select * from booking_requests where id = :id for update")
                .param("id", id)
                .query((rs, row) -> toBooking(rs))
                .optional();
    }

    @Override
    public Booking saveBooking(Booking booking) {
        if (booking.id() != 0) {
            jdbc.sql("update booking_requests set status = :status, is_paid = :paid where id = :id")
                    .param("status", booking.status().name())
                    .param("paid", booking.isPaid())
                    .param("id", booking.id())
                    .update();
            return booking;
        }
        try {
            return jdbc.sql("""
                    insert into booking_requests(user_id, session_id, status, is_paid, comment, created_at)
                    values (:student, :stay, :status, :paid, :comment, :created) returning *
                    """)
                    .param("student", booking.studentId())
                    .param("stay", booking.sessionId())
                    .param("status", booking.status().name())
                    .param("paid", booking.isPaid())
                    .param("comment", booking.comment())
                    .param("created", booking.createdAt())
                    .query((rs, row) -> toBooking(rs))
                    .single();
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("A request already exists for this session", exception);
        }
    }

    @Override
    public List<BookingView> listForStudent(long studentId) {
        return jdbc.sql(VIEW + " where b.user_id = :id order by b.id desc")
                .param("id", studentId)
                .query(VIEW_MAPPER)
                .list();
    }

    @Override
    public List<BookingView> listForStaff(Booking.Status status) {
        if (status == null) {
            return jdbc.sql(VIEW + " order by b.id desc")
                    .query(VIEW_MAPPER)
                    .list();
        }

        return jdbc.sql(VIEW + " where b.status = :status order by b.id desc")
                .param("status", status.name())
                .query(VIEW_MAPPER)
                .list();
    }

    @Override
    public BookingView view(Booking booking) {
        return jdbc.sql(VIEW + " where b.id = :id")
                .param("id", booking.id())
                .query(VIEW_MAPPER)
                .single();
    }

    @Override
    public boolean bookingExists(long bookingId) {
        return jdbc.sql("select exists(select 1 from booking_requests where id = :id)")
                .param("id", bookingId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public List<Attachment> listAttachments(long bookingId) {
        return jdbc.sql("select id, file_name, file_path from documents where request_id = :id order by id")
                .param("id", bookingId)
                .query(ATTACHMENT_MAPPER)
                .list();
    }

    @Override
    public Optional<Attachment> findAttachment(long id) {
        return jdbc.sql("select id, file_name, file_path from documents where id = :id")
                .param("id", id)
                .query(ATTACHMENT_MAPPER)
                .optional();
    }

    @Override
    public void addAttachment(long bookingId, String name, String key) {
        jdbc.sql("insert into documents(request_id, file_name, file_path) values (:id, :name, :key)")
                .param("id", bookingId)
                .param("name", name)
                .param("key", key)
                .update();
    }

    private static Stay toStay(ResultSet rs, String idColumn) throws SQLException {
        return new Stay(rs.getLong(idColumn), rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class), rs.getInt("capacity"), rs.getInt("booked_count"));
    }

    private static Booking toBooking(ResultSet rs) throws SQLException {
        return new Booking(rs.getLong("id"), rs.getLong("user_id"), rs.getLong("session_id"),
                Booking.Status.valueOf(rs.getString("status")), rs.getBoolean("is_paid"),
                rs.getString("comment"), rs.getObject("created_at", LocalDateTime.class));
    }
}
