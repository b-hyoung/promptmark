package local.promptmark.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DataSourceProvider {

    private static volatile HikariDataSource instance;

    private DataSourceProvider() {}

    public static synchronized DataSource init(Env env) {
        if (instance != null) return instance;
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(env.getRequired("DB_URL"));
        cfg.setUsername(env.getRequired("DB_USER"));
        cfg.setPassword(env.getOrDefault("DB_PASSWORD", ""));
        cfg.setMaximumPoolSize(env.getInt("DB_POOL_MAX", 10));
        cfg.setMinimumIdle(env.getInt("DB_POOL_MIN", 2));
        cfg.setConnectionTimeout(env.getInt("DB_TIMEOUT_MS", 5000));
        cfg.setPoolName("promptmark-pool");
        // Pin every pooled connection's search_path so unqualified table names
        // resolve inside the promptmark schema (shared DB with other apps).
        cfg.setConnectionInitSql("SET search_path TO promptmark, public, extensions");
        instance = new HikariDataSource(cfg);
        return instance;
    }

    public static DataSource get() {
        if (instance == null) {
            throw new IllegalStateException("DataSource not initialized — call init() first");
        }
        return instance;
    }

    public static void close() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
