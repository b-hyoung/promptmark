package local.promptmark;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import javax.sql.DataSource;

import local.promptmark.boot.AdminSeeder;
import local.promptmark.boot.SchemaApplier;
import local.promptmark.config.DataSourceProvider;
import local.promptmark.config.Env;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DevServer {

    private static final Logger log = LoggerFactory.getLogger(DevServer.class);

    private DevServer() {}

    public static void main(String[] args) throws Exception {
        Env env = Env.load();

        DataSource ds = DataSourceProvider.init(env);
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
        AdminSeeder.seed(ds,
            env.getOrDefault("ADMIN_EMAIL", ""),
            env.getOrDefault("ADMIN_PWD", ""));

        int requestedPort = env.getInt("PORT", 8080);
        int port = findAvailablePort(requestedPort);
        String contextPath = env.getOrDefault("CONTEXT_PATH", "/promptmark");

        File webappDir = new File("src/main/webapp").getAbsoluteFile();
        if (!webappDir.isDirectory()) {
            throw new IllegalStateException("Webapp dir not found: " + webappDir);
        }

        Tomcat tomcat = new Tomcat();
        File workDir = new File("build/tomcat");
        workDir.mkdirs();
        tomcat.setBaseDir(workDir.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();

        Context ctx = tomcat.addWebapp(contextPath, webappDir.getAbsolutePath());
        ctx.setReloadable(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { tomcat.stop(); } catch (Exception ignored) {}
            DataSourceProvider.close();
            log.info("promptmark stopped");
        }));

        tomcat.start();
        if (port != requestedPort) {
            log.warn("Port {} busy, using {}", requestedPort, port);
        }
        log.info("promptmark started at http://localhost:{}{}/", port, contextPath);
        tomcat.getServer().await();
    }

    private static int findAvailablePort(int startingPort) {
        for (int port = startingPort; port < startingPort + 20; port++) {
            try (ServerSocket s = new ServerSocket(port)) {
                return port;
            } catch (IOException ignored) {}
        }
        throw new IllegalStateException("No available port from " + startingPort);
    }
}
