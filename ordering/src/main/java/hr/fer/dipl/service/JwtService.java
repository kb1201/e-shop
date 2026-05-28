package hr.fer.dipl.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Read-only JWT helper: verifies RS256 signatures using the shared public key.
 * Token <em>generation</em> (private key) lives exclusively in the user service.
 *
 * <p>Under normal operation the API Gateway validates the token first and
 * injects {@code X-User-Id}/{@code X-User-Role} headers.  This service re-
 * validates the token as a defense-in-depth measure so that requests arriving
 * directly (bypassing the gateway) are still properly authenticated.
 */
@Service
public class JwtService {

    private final PublicKey publicKey;

    public JwtService(@Value("${security.jwt.public-key-path}") Resource publicKeyResource) throws Exception {
        this.publicKey = loadPublicKey(publicKeyResource);
    }

    // -------------------------------------------------------------------------
    // Claim extraction
    // -------------------------------------------------------------------------

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public Long extractUserId(String token) {
        Object raw = extractAllClaims(token).get("userId");
        if (raw instanceof Integer) return ((Integer) raw).longValue();
        if (raw instanceof Long)    return (Long) raw;
        return null;
    }

    public Collection<? extends GrantedAuthority> getAuthorities(Claims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        try {
            Object roles = claims.get("roles");
            if (roles instanceof List<?>) {
                for (Object role : (List<?>) roles) {
                    if (role instanceof String) {
                        authorities.add(new SimpleGrantedAuthority((String) role));
                    }
                }
            }
        } catch (Exception ignored) { /* fallback: empty list */ }
        return authorities;
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /** Returns {@code true} when the token has a valid signature and is not expired. */
    public boolean isTokenValid(String token) {
        try {
            return !extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // -------------------------------------------------------------------------
    // Key loading
    // -------------------------------------------------------------------------

    private PublicKey loadPublicKey(Resource resource) throws Exception {
        byte[] bytes = resource.getInputStream().readAllBytes();
        String pem = new String(bytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Decoders.BASE64.decode(pem));
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}

