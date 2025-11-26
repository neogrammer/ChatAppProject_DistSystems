package ink.bluballz.chat.auth.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Db {
  private final HikariDataSource ds;

  public Db(String url, String user, String pass) {
    var cfg = new HikariConfig();
    cfg.setJdbcUrl(url);
    cfg.setUsername(user);
    cfg.setPassword(pass);
    cfg.setMaximumPoolSize(10);
    cfg.setMinimumIdle(1);
    cfg.setPoolName("auth-hikari");
    this.ds = new HikariDataSource(cfg);
  }

  public DataSource ds() { return ds; }
}
