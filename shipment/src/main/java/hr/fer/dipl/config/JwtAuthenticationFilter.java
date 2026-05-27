package hr.fer.dipl.config;

import hr.fer.dipl.service.JwtService;
import io.jsonwebtoken.Claims;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;


    public JwtAuthenticationFilter(
            JwtService jwtService
    ) {
        this.jwtService = jwtService;
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> "token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String jwt = resolveToken(request);
        final String username;

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract username from JWT token
            username = jwtService.extractUsername(jwt);

            // If username exists and user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate the token
                if (jwtService.isTokenValid(jwt)) {
                    // Extract claims and authorities
                    Claims claims = jwtService.extractAllClaims(jwt);
                    var userId = jwtService.extractUserId(jwt);
                    Collection<? extends GrantedAuthority> authorities = jwtService.getAuthorities(claims);

                    // Create authentication token with username, null credentials, and authorities
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

                    // Add request details to authentication token
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log exception but don't stop the filter chain
            // logger.error("Could not set user authentication in security context", e);
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}