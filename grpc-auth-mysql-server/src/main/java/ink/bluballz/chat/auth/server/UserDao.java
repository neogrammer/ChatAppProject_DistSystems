package ink.bluballz.chat.auth.server;

import java.sql.*;
import java.time.Instant;

public class UserDao {
  private final Db db;
  public record User(long id, String email, String displayName, String passwordHash, String refreshHash, Instant refreshExpires) {}
  public UserDao(Db db) { this.db = db; }

  public boolean existsByEmail(String email) throws Exception {
    try (var c = db.ds().getConnection(); var ps = c.prepareStatement("SELECT 1 FROM users WHERE email=?")) {
      ps.setString(1, email);
      try (var rs = ps.executeQuery()) { return rs.next(); }
    }
  }

  public long insertUser(String email, String display, String pwdHash) throws Exception {
    try (var c = db.ds().getConnection();
         var ps = c.prepareStatement("INSERT INTO users(email,display_name,password_hash) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, email);
      ps.setString(2, display);
      ps.setString(3, pwdHash);
      ps.executeUpdate();
      try (var rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
    }
  }

  public User findByEmail(String email) throws Exception {
    try (var c = db.ds().getConnection(); var ps = c.prepareStatement("SELECT id,email,display_name,password_hash,refresh_hash,refresh_expires FROM users WHERE email=?")) {
      ps.setString(1, email);
      try (var rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        return map(rs);
      }
    }
  }

  public User findByRefresh(String refreshHash) throws Exception {
    try (var c = db.ds().getConnection(); var ps = c.prepareStatement("SELECT id,email,display_name,password_hash,refresh_hash,refresh_expires FROM users WHERE refresh_hash=?")) {
      ps.setString(1, refreshHash);
      try (var rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        return map(rs);
      }
    }
  }

  public void updateRefresh(long userId, String refreshHash, Instant expires) throws Exception {
    try (var c = db.ds().getConnection(); var ps = c.prepareStatement("UPDATE users SET refresh_hash=?, refresh_expires=? WHERE id=?")) {
      ps.setString(1, refreshHash);
      ps.setTimestamp(2, Timestamp.from(expires));
      ps.setLong(3, userId);
      ps.executeUpdate();
    }
  }

  private static User map(ResultSet rs) throws Exception {
    var exp = rs.getTimestamp("refresh_expires");
    return new User(
        rs.getLong("id"),
        rs.getString("email"),
        rs.getString("display_name"),
        rs.getString("password_hash"),
        rs.getString("refresh_hash"),
        exp == null ? null : exp.toInstant());
  }
}