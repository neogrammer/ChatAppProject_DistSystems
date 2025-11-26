package ink.bluballz.chat.auth.server;

import ink.bluballz.chat.auth.v1.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {
  private final UserDao userDao;
  private final JwtUtil jwt;
  private final SecureRandom rng = new SecureRandom();

  public AuthServiceImpl(UserDao userDao, JwtUtil jwt) {
    this.userDao = userDao;
    this.jwt = jwt;
  }

  @Override
  public void register(RegisterRequest req, StreamObserver<AuthResponse> resp) {
    try {
      var email = req.getEmail().trim().toLowerCase();
      var display = req.getDisplayName().isBlank() ? email : req.getDisplayName().trim();
      if (!EmailValidator.isValid(email)) {
        resp.onError(Status.INVALID_ARGUMENT.withDescription("Invalid email").asRuntimeException());
        return;
      }
      if (req.getPassword().length() < 8) {
        resp.onError(Status.INVALID_ARGUMENT.withDescription("Password too short").asRuntimeException());
        return;
      }
      if (userDao.existsByEmail(email)) {
        resp.onError(Status.ALREADY_EXISTS.withDescription("Email in use").asRuntimeException());
        return;
      }
      var hash = PasswordUtil.hash(req.getPassword());
      var userId = userDao.insertUser(email, display, hash);
      var tokens = issueTokensFor(userId, email, display);
      var out = AuthResponse.newBuilder()
          .setUserId(Long.toString(userId))
          .setEmail(email)
          .setDisplayName(display)
          .setTokens(tokens)
          .build();
      resp.onNext(out);
      resp.onCompleted();
    } catch (Exception e) {
      resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void login(LoginRequest req, StreamObserver<AuthResponse> resp) {
    try {
      var email = req.getEmail().trim().toLowerCase();
      var user = userDao.findByEmail(email);
      if (user == null) {
        resp.onError(Status.NOT_FOUND.withDescription("No such user").asRuntimeException());
        return;
      }
      if (!PasswordUtil.verify(req.getPassword(), user.passwordHash())) {
        resp.onError(Status.PERMISSION_DENIED.withDescription("Bad credentials").asRuntimeException());
        return;
      }
      var tokens = issueTokensFor(user.id(), user.email(), user.displayName());
      var out = AuthResponse.newBuilder()
          .setUserId(Long.toString(user.id()))
          .setEmail(user.email())
          .setDisplayName(user.displayName())
          .setTokens(tokens)
          .build();
      resp.onNext(out);
      resp.onCompleted();
    } catch (Exception e) {
      resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void refresh(RefreshRequest req, StreamObserver<AuthTokens> resp) {
    try {
      var token = req.getRefreshToken();
      if (token.isBlank()) {
        resp.onError(Status.INVALID_ARGUMENT.withDescription("Missing refresh token").asRuntimeException());
        return;
      }
      var hash = JwtUtil.sha256(token);
      var user = userDao.findByRefresh(hash);
      if (user == null) {
        resp.onError(Status.PERMISSION_DENIED.withDescription("Invalid refresh token").asRuntimeException());
        return;
      }
      if (user.refreshExpires() != null && user.refreshExpires().isBefore(Instant.now())) {
        resp.onError(Status.PERMISSION_DENIED.withDescription("Refresh expired").asRuntimeException());
        return;
      }
      var tokens = issueTokensFor(user.id(), user.email(), user.displayName());
      resp.onNext(tokens);
      resp.onCompleted();
    } catch (Exception e) {
      resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void validateToken(ValidateTokenRequest req, StreamObserver<ValidateTokenResponse> resp) {
    try {
      var claims = jwt.parseAccess(req.getToken());
      var out = ValidateTokenResponse.newBuilder()
          .setValid(true)
          .setUserId(claims.getSubject())
          .setEmail((String) claims.get("email"))
          .build();
      resp.onNext(out);
      resp.onCompleted();
    } catch (Exception e) {
      resp.onNext(ValidateTokenResponse.newBuilder().setValid(false).build());
      resp.onCompleted();
    }
  }

  private AuthTokens issueTokensFor(long userId, String email, String display) throws Exception {
    var access = jwt.issueAccess(Long.toString(userId), email, display);
    var refreshRaw = randomHex(32);
    var refreshExp = Instant.now().plus(jwt.refreshDays(), ChronoUnit.DAYS);
    userDao.updateRefresh(userId, JwtUtil.sha256(refreshRaw), refreshExp);
    return AuthTokens.newBuilder()
        .setAccessToken(access.token())
        .setRefreshToken(refreshRaw)
        .setAccessExpiresAt(access.expiresAt())
        .setRefreshExpiresAt(refreshExp.getEpochSecond())
        .build();
  }

  private String randomHex(int bytes) {
    var arr = new byte[bytes];
    rng.nextBytes(arr);
    return HexFormat.of().formatHex(arr);
  }

  static class EmailValidator { static boolean isValid(String e){ return e != null && e.contains("@") && e.contains("."); } }
}