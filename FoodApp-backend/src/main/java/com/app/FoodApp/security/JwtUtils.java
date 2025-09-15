package com.app.FoodApp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
public class JwtUtils {
    // Token validity: 30 days (in milliseconds)
    private static final long EXPIRATION_TIME = 30L * 24 * 60 * 60 * 1000;

    // Key used to sign and verify JWT tokens
    private SecretKey secretKey;

    // Secret string loaded from application.properties or application.yml
    @Value("${secretJwtString}")
    private String secretJwtString;

    /**
     * Initializes the secret key after the bean is constructed.
     * Converts the configured secret string into a secure HMAC-SHA256 key.
     */
    @PostConstruct
    private void init() {
        // Convert secret string into bytes and generate HMAC-SHA256 key
        this.secretKey = Keys.hmacShaKeyFor(secretJwtString.getBytes(StandardCharsets.UTF_8));
        log.info("JWT secret key initialized successfully");
    }

    /**
     * Generates a JWT token with email as the subject.
     *
     * @param email - user's email to set as token subject
     * @return signed JWT token
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email) // set subject (user identifier)
                .issuedAt(new Date(System.currentTimeMillis())) // when token was issued
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // expiry date
                .signWith(secretKey) // sign using HS256 with the secret key
                .compact();
    }

    /**
     * Extracts the username (email) from a JWT token.
     *
     * @param token - JWT token
     * @return subject (username/email)
     */
    public String getUserNameFromToken(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    /**
     * Validates a token by checking:
     * 1. If the username matches the authenticated user
     * 2. If the token is not expired
     *
     * @param token - JWT token
     * @param userDetails - Spring Security's UserDetails
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUserNameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Extracts any claim from the token using a resolver function.
     * Example: subject, expiration date, etc.
     *
     * @param token - JWT token
     * @param claimsResolver - function to extract a specific claim
     * @return extracted claim
     */
    private <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(
                Jwts.parser()
                        .verifyWith(secretKey) // verify token with signing key
                        .build()
                        .parseSignedClaims(token) // parse and validate signature
                        .getPayload() // return claims (payload)
        );
    }

    /**
     * Checks if the token is expired by comparing expiration date with current time.
     *
     * @param token - JWT token
     * @return true if expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }
}

