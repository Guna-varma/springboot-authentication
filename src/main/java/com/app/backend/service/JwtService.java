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




//package com.app.backend.service;
//
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.nio.charset.StandardCharsets;
//import java.security.Key;
//import java.util.Date;
//import java.util.UUID;
//import java.util.function.Function;
//
//@Service
//@Slf4j
//public class JwtService {
//
//    @Value("${jwt.secret}")
//    private String secret;
//
//    @Value("${jwt.expiration:900000}") // 15 minutes default
//    private long expirationTime;
//
//    private Key getSigningKey() {
//        if (secret == null || secret.trim().isEmpty()) {
//            log.error("JWT Secret is null or empty! Check your configuration.");
//            throw new IllegalStateException("JWT Secret not configured");
//        }
//        log.debug("Using JWT secret length: {}", secret.length());
//        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
//    }
//
//    public String generateToken(String username) {
//        return generateTokenWithId(username);
//    }
//
//    public String generateTokenWithId(String username) {
//        String tokenId = UUID.randomUUID().toString();
//        Date now = new Date();
//        Date expiry = new Date(now.getTime() + expirationTime);
//
//        log.debug("Generating token for user: {}, expires at: {}", username, expiry);
//
//        return Jwts.builder()
//                .setId(tokenId)
//                .setSubject(username)
//                .setIssuedAt(now)
//                .setExpiration(expiry)
//                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    public boolean isTokenValid(String token, UserDetails userDetails) {
//        try {
//            String username = extractUsername(token);
//            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
//            log.debug("Token validation for user {}: {}", username, isValid);
//            return isValid;
//        } catch (Exception e) {
//            log.error("Token validation failed: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    public String extractUsername(String token) {
//        return extractClaim(token, Claims::getSubject);
//    }
//
//    public String getTokenId(String token) {
//        return extractClaim(token, Claims::getId);
//    }
//
//    public Date extractExpiration(String token) {
//        return extractClaim(token, Claims::getExpiration);
//    }
//
//    private boolean isTokenExpired(String token) {
//        return extractExpiration(token).before(new Date());
//    }
//
//    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        try {
//            Claims claims = Jwts.parserBuilder()
//                    .setAllowedClockSkewSeconds(60) // Allow 60s time skew
//                    .setSigningKey(getSigningKey())
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//            return claimsResolver.apply(claims);
//        } catch (SignatureException e) {
//            log.error("JWT signature validation failed: {}", e.getMessage());
//            throw e;
//        } catch (ExpiredJwtException e) {
//            log.error("JWT token expired: {}", e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            log.error("JWT parsing failed: {}", e.getMessage());
//            throw e;
//        }
//    }
//}