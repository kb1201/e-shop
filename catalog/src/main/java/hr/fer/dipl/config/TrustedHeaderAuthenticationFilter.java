package hr.fer.dipl.config;

import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads the X-User-Id / X-User-Role headers injected by the API Gateway
 * and establishes a Spring Security authentication context.
 * <p>
 * These headers are set (and any client-supplied copies stripped) by
 * {@code JwtAuthenticationGlobalFilter} in the gateway — no JWT parsing
 * is needed here.
 */
public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-Id");
        String rolesHeader  = request.getHeader("X-User-Role");

        if (userIdHeader != null && !userIdHeader.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = Long.parseLong(userIdHeader.trim());

                List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                if (rolesHeader != null && !rolesHeader.isBlank()) {
                    authorities = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (NumberFormatException ignored) {
                // Malformed header — leave security context empty; Spring Security
                // will reject the request if the endpoint requires authentication.
            }
        }

        filterChain.doFilter(request, response);
    }
}
