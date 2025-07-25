package com.app.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:900000}") // 15 minutes default
    private long expirationTime;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

//    public String generateToken(String username) {
//        return Jwts.builder()
//                .setSubject(username)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Use config value!
//                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }

    // Keep your existing methods...
    public String generateToken(String username) {
        return generateTokenWithId(username); // Use the enhanced version
    }

    // Add method to get token ID for blacklisting
    public String getTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

//    public String generateTokenWithId(String username) {
//        String tokenId = UUID.randomUUID().toString();
//        return Jwts.builder()
//                .setId(tokenId) // Add unique ID for blacklisting
//                .setSubject(username)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
//                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }

    public String generateTokenWithId(String username) {
        String tokenId = UUID.randomUUID().toString();
        return Jwts.builder()
                .setId(tokenId) // Unique ID for blacklisting
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setAllowedClockSkewSeconds(60) // allow 60s time skew
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

//    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//        return claimsResolver.apply(claims);
//    }

}