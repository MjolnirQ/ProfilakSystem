package ru.nsu.resortbooking.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.resortbooking.adapter.security.HttpSessionAuthentication;
import ru.nsu.resortbooking.application.AccountService;
import ru.nsu.resortbooking.domain.Account;

import java.util.Collection;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AccountService accounts;
    private final HttpSessionAuthentication sessions;

    public AuthController(AccountService accounts, HttpSessionAuthentication sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public RegistrationResponse register(@RequestBody Credentials request) {
        Account student = accounts.registerStudent(request.email(), request.password());
        return new RegistrationResponse(student.id(), student.email(), student.role());
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody Credentials credentials,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = sessions.login(
                credentials.email(),
                credentials.password(),
                request,
                response
        );

        String role = authentication.getAuthorities().iterator().next().getAuthority();
        return new LoginResponse(authentication.getName(), role);
    }

    @GetMapping("/me")
    public ProfileResponse profile(Authentication authentication) {
        return new ProfileResponse(authentication.getName(), authentication.getAuthorities());
    }

    public record Credentials(String email, String password) {
    }

    public record RegistrationResponse(long id, String email, Account.Role role) {
    }

    public record LoginResponse(String email, String role) {
    }

    public record ProfileResponse(String email, Collection<? extends GrantedAuthority> roles) {
    }
}
