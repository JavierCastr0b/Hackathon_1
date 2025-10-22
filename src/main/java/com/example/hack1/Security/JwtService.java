package com.example.hack1.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-access}")
    private Long accessTokenExpiration;

    @Value("${jwt.expiration-refresh}")
    private Long refreshTokenExpiration;

    private final JwtParser jwtParser;


    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
        this.jwtParser = Jwts.parser().setSigningKey(getSigningKey()).build();
    }


    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }


    public boolean isTokenValid(String token) {
        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token es inválido o expirado
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }


    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }


}
