package hr.fer.dipl.service;

import hr.fer.dipl.db.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.function.Function;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
public class JwtService {

    private final long jwtExpiration;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public JwtService(
            @Value("${security.jwt.private-key-path}") Resource privateKeyResource,
            @Value("${security.jwt.public-key-path}") Resource publicKeyResource,
            @Value("${security.jwt.expiration-time}") long jwtExpiration
    ) throws Exception {
        this.publicKey = getPublicKey(publicKeyResource);
        this.privateKey = getPrivateKey(privateKeyResource);
        this.jwtExpiration = jwtExpiration;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object raw = claims.get("userId");
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

    public Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(this.publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObject = claims.get("roles");
        if (rolesObject instanceof List<?>) {
            return ((List<?>) rolesObject).stream()
                    .filter(item -> item instanceof String)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        extraClaims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

        // Assuming your UserDetails implementation has getId()
        if (userDetails instanceof User customUser) {
            extraClaims.put("userId", customUser.getId());
        }

        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /** Full validation against a known UserDetails (used during login). */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Signature + expiry validation without a DB lookup.
     * Used by {@link hr.fer.dipl.config.JwtAuthenticationFilter} as defense-in-depth
     * on every incoming request â€” no round-trip to the user table required.
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(this.privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    private PrivateKey getPrivateKey(Resource privateKeyResource) {
        try {
            byte[] keyBytes = privateKeyResource.getInputStream().readAllBytes();
            String privateKeyPem = new String(keyBytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Decoders.BASE64.decode(privateKeyPem));
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (IOException | RuntimeException | java.security.GeneralSecurityException e) {
            throw new RuntimeException("Could not load private key", e);
        }
    }

    private PublicKey getPublicKey(Resource publicKeyResource) {
        try {
            byte[] keyBytes = publicKeyResource.getInputStream().readAllBytes();
            String publicKeyPem = new String(keyBytes)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Decoders.BASE64.decode(publicKeyPem));
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (IOException | RuntimeException | java.security.GeneralSecurityException e) {
            throw new RuntimeException("Could not load public key", e);
        }
    }
}
