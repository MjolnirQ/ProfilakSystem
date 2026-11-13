package ru.nsu.resortbooking.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import ru.nsu.resortbooking.domain.Account;

@Component
public class HttpSessionAuthentication {
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessions;
    private final SecurityContextRepository contexts;

    public HttpSessionAuthentication(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessions,
            SecurityContextRepository contexts
    ) {
        this.authenticationManager = authenticationManager;
        this.sessions = sessions;
        this.contexts = contexts;
    }

    public Authentication login(
            String email,
            String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authenticated = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        Account.normalizeEmail(email),
                        password
                )
        );

        sessions.onAuthentication(authenticated, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);

        return authenticated;
    }
}
