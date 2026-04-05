package com.AJJ.ms_security.Services;


import com.AJJ.ms_security.Models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    // HU-009: el token incluye ID usuario, roles, timestamp creación y expiración
    public String generateToken(User theUser, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("_id", theUser.getId());
        claims.put("name", theUser.getName());
        claims.put("email", theUser.getEmail());
        claims.put("roles", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(theUser.getName())
                .setIssuedAt(now)         // timestamp de creación
                .setExpiration(expiryDate) // timestamp de expiración
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
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();

            User theUser = new User();
            // Fix: la clave guardada es "_id", no "id"
            theUser.setId((String) claims.get("_id"));
            theUser.setName((String) claims.get("name"));
            theUser.setEmail((String) claims.get("email"));
            return theUser;
        } catch (Exception e) {
            return null;
        }
    }
}
