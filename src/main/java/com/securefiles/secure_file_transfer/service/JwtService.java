package com.securefiles.secure_file_transfer.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

  private final SecretKey key;
  private final int expMinutes;

  public JwtService(
      @Value("${app.jwt.secretBase64}") String secretB64,
      @Value("${app.jwt.expMinutes:60}") int expMinutes
  ) {
    byte[] secret = Base64.getDecoder().decode(secretB64);
    this.key = Keys.hmacShaKeyFor(secret);
    this.expMinutes = expMinutes;
  }

  public String issueToken(String username) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(expMinutes * 60L);

    return Jwts.builder()
        .subject(username)
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public String parseUsername(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }
}
