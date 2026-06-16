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
    private static final String ONBOARDING_TOKEN_TYPE = "OAUTH_ONBOARDING";
    private static final String GOOGLE_ONBOARDING_TOKEN_TYPE = "GOOGLE_ONBOARDING";
    private static final long ONBOARDING_EXPIRATION_MS = 15 * 60 * 1000;

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

    /**
     * Token de onboarding agnostico al proveedor OAuth (google, github,
     * microsoft). Lleva el provider en los claims para que el endpoint
     * que completa el perfil sepa de donde vino.
     */
    public String generateOnboardingToken(User theUser, String provider) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ONBOARDING_EXPIRATION_MS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("_id", theUser.getId());
        claims.put("name", theUser.getName());
        claims.put("email", theUser.getEmail());
        claims.put("provider", provider);
        claims.put("type", ONBOARDING_TOKEN_TYPE);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(theUser.getName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // Compat: tokens emitidos antes de tener el flujo agnostico siguen
    // siendo aceptados por completeGoogleProfile.
    public String generateGoogleOnboardingToken(User theUser) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ONBOARDING_EXPIRATION_MS);

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

    public User getUserFromOnboardingToken(String token) {
        // Acepta el tipo nuevo agnostico y, por compat, el viejo de Google.
        User fromGenerico = this.getUserFromToken(token, ONBOARDING_TOKEN_TYPE);
        if (fromGenerico != null) return fromGenerico;
        return this.getUserFromToken(token, GOOGLE_ONBOARDING_TOKEN_TYPE);
    }

    /**
     * Devuelve el provider declarado en el token de onboarding (google,
     * github, microsoft). Si el token es del tipo viejo de Google, devuelve
     * "google" por defecto.
     */
    public String getProviderFromOnboardingToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();
            String type = (String) claims.get("type");

            if (ONBOARDING_TOKEN_TYPE.equals(type)) {
                Object provider = claims.get("provider");
                return provider != null ? provider.toString() : null;
            }
            if (GOOGLE_ONBOARDING_TOKEN_TYPE.equals(type)) {
                return "google";
            }
            return null;
        } catch (Exception e) {
            return null;
        }
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
