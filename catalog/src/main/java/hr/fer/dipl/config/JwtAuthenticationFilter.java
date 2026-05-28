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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Two-layer authentication filter providing defense in depth.
 *
 * <p><b>Layer 1 â€” JWT validation (direct-access protection):</b><br>
 * If the request carries an {@code Authorization: Bearer <token>} header or a
 * {@code token} cookie, the token is validated locally using the RSA public key.
 * A valid token sets the security context from its own claims.
 * An <em>invalid</em> token short-circuits here â€” it does <em>not</em> fall
 * through to the header layer, preventing an attacker from bypassing validation
 * by deliberately sending a bad token alongside forged headers.
 *
 * <p><b>Layer 2 â€” Trusted gateway headers (normal gateway path):</b><br>
 * Reached only when <em>no JWT is present at all</em>.  The API Gateway
 * ({@code JwtAuthenticationGlobalFilter}) validates the token once, strips any
 * client-supplied {@code X-User-Id}/{@code X-User-Role} headers, and re-injects
 * them from the validated claims before forwarding the request.  This layer
 * trusts those injected headers.
 *
 * <p><b>Security boundary note:</b> Layer 2 is fully secure only when services
 * are not reachable directly from outside (e.g., via Kubernetes NetworkPolicy or
 * a private subnet).  Without network isolation a direct caller could omit the
 * JWT and send fabricated headers.  Layer 1 exists precisely to protect against
 * that scenario.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = resolveToken(request);

        if (jwt != null) {
            // --- Layer 1: JWT present â€” validate it ---
            // Do NOT fall through to Layer 2 when the token is invalid; that would
            // let an attacker force the header path by sending a deliberately bad token.
            if (jwtService.isTokenValid(jwt)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    Claims claims    = jwtService.extractAllClaims(jwt);
                    Long  userId     = jwtService.extractUserId(jwt);
                    Collection<? extends GrantedAuthority> authorities = jwtService.getAuthorities(claims);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) {
                    // Parsing failed after isTokenValid â€” leave context empty
                }
            }
        } else {
            // --- Layer 2: No JWT â€” trust gateway-injected headers ---
            // The gateway strips client-supplied copies of these headers before
            // injecting its own, so this path is safe when services sit behind it.
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
                    // Malformed header â€” stay anonymous
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Token resolution: Authorization: Bearer <token>  â†’  fallback: "token" cookie
    // -------------------------------------------------------------------------
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
}
