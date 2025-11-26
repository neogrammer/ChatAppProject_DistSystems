package ink.bluballz.chat.auth.server;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
  private final SecretKey key;
  private final int accessMinutes;
  private final int refreshDays;

  public record Access(String token, long expiresAt) {}

  public JwtUtil(String secret, int accessMinutes, int refreshDays) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessMinutes = accessMinutes;
    this.refreshDays = refreshDays;
  }

  public Access issueAccess(String userId, String email, String display) {
    var now = Instant.now();
    var exp = now.plusSeconds(accessMinutes * 60L);
    var token = Jwts.builder()
        .setSubject(userId)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .addClaims(Map.of("email", email, "name", display))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
    return new Access(token, exp.getEpochSecond());
  }

  public Claims parseAccess(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
  }

  public int refreshDays() { return refreshDays; }

  public static String sha256(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) { throw new RuntimeException(e); }
  }
}