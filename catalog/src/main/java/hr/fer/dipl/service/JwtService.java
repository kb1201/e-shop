package hr.fer.dipl.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;


@Service
public class JwtService {

    private final PublicKey publicKey;

    public JwtService(@Value("${security.jwt.public-key-path}") Resource publicKeyResource) throws Exception {
        this.publicKey = getPublicKey(publicKeyResource);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT", e);
        }
    }

    private PublicKey getPublicKey(Resource publicKeyResource) throws Exception {
        byte[] keyBytes = publicKeyResource.getInputStream().readAllBytes();
        String publicKeyPem = new String(keyBytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Decoders.BASE64.decode(publicKeyPem));
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }


    public Collection<? extends GrantedAuthority> getAuthorities(Claims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Try to extract roles from claims - common formats
        try {
            // Try to get from "roles" claim
            if (claims.get("roles") instanceof List) {
                List<String> roles = (List<String>) claims.get("roles");
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }

        } catch (Exception e) {
            // Fallback to empty authorities if extraction fails
            e.printStackTrace();
            // logger.warn("Failed to extract authorities from JWT: " + e.getMessage());
        }

        return authorities;
    }
}
