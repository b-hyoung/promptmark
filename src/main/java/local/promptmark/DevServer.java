package local.promptmark;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DevServer {

    private static final Logger log = LoggerFactory.getLogger(DevServer.class);

    private DevServer() {}

    public static void main(String[] args) throws Exception {
        int requestedPort = Integer.parseInt(System.getProperty("port", "8080"));
        int port = findAvailablePort(requestedPort);
        String contextPath = System.getProperty("contextPath", "/promptmark");

        File webappDir = new File("src/main/webapp").getAbsoluteFile();
        if (!webappDir.isDirectory()) {
            throw new IllegalStateException("Webapp directory not found: " + webappDir);
        }

        Tomcat tomcat = new Tomcat();
        File workDir = new File("build/tomcat");
        workDir.mkdirs();
        tomcat.setBaseDir(workDir.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();

        Context ctx = tomcat.addWebapp(contextPath, webappDir.getAbsolutePath());
        ctx.setReloadable(true);

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
            } catch (IOException ignored) { /* try next */ }
        }
        throw new IllegalStateException(
            "No available port from " + startingPort + " to " + (startingPort + 19));
    }
}
