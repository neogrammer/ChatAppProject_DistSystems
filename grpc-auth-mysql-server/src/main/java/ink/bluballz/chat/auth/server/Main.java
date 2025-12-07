package ink.bluballz.chat.auth.server;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.util.Optional;

public class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    var port = Integer.parseInt(env("PORT", "50051"));

    var db = new Db(
        env("DB_URL", "jdbc:mysql://localhost:3306/chatapp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"),
        env("DB_USER", "chatuser"),
        env("DB_PASSWORD", "chatpass")
    );

    var jwtSecret = env("JWT_SECRET", "dev_dev_dev_dev_dev_dev_dev_32+dev_dev_dev_dev");
    var accessMinutes = Integer.parseInt(env("ACCESS_MINUTES", "15"));
    var refreshDays = Integer.parseInt(env("REFRESH_DAYS", "7"));

    var jwt = new JwtUtil(jwtSecret, accessMinutes, refreshDays);
    var userDao = new UserDao(db);
    var svc = new AuthServiceImpl(userDao, jwt);

    Server server = NettyServerBuilder.forPort(port)
        .addService(svc)
        .build();

    System.out.println("AuthService listening on " + port);
    server.start();
    server.awaitTermination();
  }

  private static String env(String k, String def) { return Optional.ofNullable(System.getenv(k)).orElse(def); }
}