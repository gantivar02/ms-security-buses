package com.AJJ.ms_security.Services;


import com.AJJ.ms_security.Models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String GOOGLE_ONBOARDING_TOKEN_TYPE = "GOOGLE_ONBOARDING";
    private static final long GOOGLE_ONBOARDING_EXPIRATION_MS = 15 * 60 * 1000;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key secretKey;

    // HU-009: construye la clave desde application.properties en vez de generarla aleatoriamente
    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // HU-009: el token incluye ID usuario, roles, timestamp creación y expiración
    public String generateToken(User theUser, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("_id", theUser.getId());
        claims.put("name", theUser.getName());
        claims.put("email", theUser.getEmail());
        claims.put("roles", roles);
        claims.put("githubUsername", theUser.getGithubUsername());
        claims.put("type", ACCESS_TOKEN_TYPE);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(theUser.getName())
                .setIssuedAt(now)         // timestamp de creación
                .setExpiration(expiryDate) // timestamp de expiración
                .signWith(secretKey)
                .compact();
    }

    public String generateGoogleOnboardingToken(User theUser) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + GOOGLE_ONBOARDING_EXPIRATION_MS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("_id", theUser.getId());
        claims.put("name", theUser.getName());
        claims.put("email", theUser.getEmail());
        claims.put("type", GOOGLE_ONBOARDING_TOKEN_TYPE);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(theUser.getName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            Date now = new Date();
            if (claimsJws.getBody().getExpiration().before(now)) {
                return false;
            }
            return true;
        } catch (SignatureException ex) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public User getUserFromToken(String token) {
        return this.getUserFromToken(token, ACCESS_TOKEN_TYPE);
    }

    public User getUserFromGoogleOnboardingToken(String token) {
        return this.getUserFromToken(token, GOOGLE_ONBOARDING_TOKEN_TYPE);
    }

    private User getUserFromToken(String token, String expectedType) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();
            if (!expectedType.equals(claims.get("type"))) {
                return null;
            }

            User theUser = new User();
            theUser.setId((String) claims.get("_id"));
            theUser.setName((String) claims.get("name"));
            theUser.setEmail((String) claims.get("email"));
            return theUser;
        } catch (Exception e) {
            return null;
        }
    }
}
