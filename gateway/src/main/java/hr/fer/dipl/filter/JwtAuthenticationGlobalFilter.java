package hr.fer.dipl.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Optional;

/**
 * Gateway-level JWT authentication filter â€” the <em>primary</em> auth boundary.
 *
 * <h3>What this filter does</h3>
 * <ol>
 *   <li>Strips any client-supplied {@code X-User-Id} / {@code X-User-Role} headers
 *       to prevent spoofing.</li>
 *   <li>Validates the JWT (from {@code Authorization: Bearer} header or {@code token}
 *       cookie) using the RSA public key.</li>
 *   <li>On success: injects {@code X-User-Id} and {@code X-User-Role} headers for
 *       downstream services and <strong>forwards the original {@code Authorization}
 *       header unchanged</strong> â€” Spring Cloud Gateway passes all non-sensitive
 *       headers by default, so no extra configuration is needed.</li>
 *   <li>On failure / missing token: returns {@code 401} unless the path is in the
 *       public-path table below.</li>
 * </ol>
 *
 * <h3>Defense in depth</h3>
 * Each downstream service runs its own {@code JwtAuthenticationFilter} that
 * re-validates the forwarded {@code Authorization} header.  This means a request
 * that somehow bypasses the gateway (e.g., direct port access during development)
 * must still carry a valid JWT to be authenticated.  The {@code X-User-Id} /
 * {@code X-User-Role} headers are used only as a <em>fallback</em> when no JWT is
 * present â€” see {@code JwtAuthenticationFilter} in each service.
 *
 * <h3>Public paths (no JWT required)</h3>
 * Defined in {@link #isPublicPath}:
 * <ul>
 *   <li>{@code POST /users} â€” sign-up</li>
 *   <li>{@code POST /users/login}</li>
 *   <li>{@code POST /users/logout}</li>
 *   <li>All non-POST {@code /products/**} â€” catalog browsing</li>
 *   <li>{@code OPTIONS /**} â€” CORS preflight</li>
 * </ul>
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    private final PublicKey publicKey;

    public JwtAuthenticationGlobalFilter(
            @Value("${security.jwt.public-key-path}") Resource publicKeyResource) throws Exception {
        this.publicKey = loadPublicKey(publicKeyResource);
    }

    // Run before all gateway routing filters
    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // Strip any client-supplied identity headers to prevent spoofing
        ServerHttpRequest sanitized = request.mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                })
                .build();

        // CORS preflight â€” always pass through, no auth needed
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        Optional<String> tokenOpt = resolveToken(request);

        if (tokenOpt.isPresent()) {
            try {
                Claims claims = parseToken(tokenOpt.get());
                String userId = String.valueOf(extractUserId(claims));
                String roles  = String.join(",", extractRoles(claims));

                ServerHttpRequest enriched = sanitized.mutate()
                        .header("X-User-Id",   userId)
                        .header("X-User-Role", roles)
                        .build();

                return chain.filter(exchange.mutate().request(enriched).build());
            } catch (Exception e) {
                log.debug("JWT validation failed for {} {}: {}", method, path, e.getMessage());
                if (!isPublicPath(path, method)) {
                    return reject(exchange);
                }
            }
        } else if (!isPublicPath(path, method)) {
            return reject(exchange);
        }

        return chain.filter(exchange.mutate().request(sanitized).build());
    }

    // ---------------------------------------------------------------------------
    // Public-path table â€” the single authoritative list of unauthenticated routes
    // ---------------------------------------------------------------------------
    private boolean isPublicPath(String path, HttpMethod method) {
        if (HttpMethod.POST.equals(method) && "/users".equals(path))        return true; // signup
        if (HttpMethod.POST.equals(method) && "/users/login".equals(path))  return true;
        if (HttpMethod.POST.equals(method) && "/users/logout".equals(path)) return true;
        if (!HttpMethod.POST.equals(method) && path.startsWith("/products")) return true; // catalog browsing
        return false;
    }

    // ---------------------------------------------------------------------------
    // Token resolution: Authorization: Bearer <token>  â†’  fallback: "token" cookie
    // ---------------------------------------------------------------------------
    private Optional<String> resolveToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }
        return request.getCookies()
                .getOrDefault("token", List.of())
                .stream()
                .findFirst()
                .map(cookie -> cookie.getValue());
    }

    // ---------------------------------------------------------------------------
    // JWT helpers
    // ---------------------------------------------------------------------------
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Long extractUserId(Claims claims) {
        Object raw = claims.get("userId");
        if (raw instanceof Integer) return ((Integer) raw).longValue();
        if (raw instanceof Long)    return (Long) raw;
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?>) return (List<String>) raw;
        return List.of();
    }

    // ---------------------------------------------------------------------------
    // RSA public-key loader
    // ---------------------------------------------------------------------------
    private PublicKey loadPublicKey(Resource resource) throws Exception {
        byte[] bytes = resource.getInputStream().readAllBytes();
        String pem = new String(bytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Decoders.BASE64.decode(pem));
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
