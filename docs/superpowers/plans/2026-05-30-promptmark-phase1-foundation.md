# promptmark Implementation Plan — Phase 1: Foundation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up a bootable promptmark project skeleton: Gradle build → embedded Tomcat serving a placeholder JSP → Supabase Postgres connection pool → schema applied → ADMIN account seeded → structured logging with traceId.

**Architecture:** MVC2-style web app using bare servlet API + JSP/JSTL. No Spring/Hibernate. HikariCP for DB. Logback with MDC for traceId. All wiring done in `DevServer` (Tomcat embed bootstrap) and tiny config classes.

**Tech Stack:** Java 11+ (compile target 1.8 for compatibility), Gradle 8.x, Tomcat 9 embed (javax.\*), JSTL 1.2, PostgreSQL 42.7, HikariCP 5.1, jBCrypt 0.4, Logback 1.5, JUnit 5, Testcontainers (pgvector image).

**Spec reference:** `docs/superpowers/specs/2026-05-30-promptmark-design.md` §0–§2, §6.

---

## File Structure (created in this phase)

```
jvisin_jsp_web/
├── build.gradle                                        # Gradle config + deps
├── settings.gradle                                     # Root project name
├── gradlew, gradlew.bat, gradle/wrapper/*              # Gradle wrapper (generated)
├── .env.example                                        # Env var template
├── libs/                                               # (kept for cos.jar in Phase 3)
└── src/
    ├── main/
    │   ├── java/local/promptmark/
    │   │   ├── DevServer.java                          # Tomcat embed bootstrap
    │   │   ├── config/
    │   │   │   ├── Env.java                            # .env + System.getenv() loader
    │   │   │   └── DataSourceProvider.java             # HikariCP pool singleton
    │   │   ├── web/
    │   │   │   ├── AppException.java                   # Base + 5 subclasses
    │   │   │   ├── TraceIdFilter.java                  # X-Trace-Id header + MDC
    │   │   │   └── HelloServlet.java                   # Placeholder GET / → "It works"
    │   │   └── boot/
    │   │       ├── SchemaApplier.java                  # Applies V1__init.sql at startup
    │   │       └── AdminSeeder.java                    # Upserts ADMIN row from env
    │   ├── resources/
    │   │   ├── logback.xml                             # Console + rolling file appenders
    │   │   └── db/migration/V1__init.sql               # Full DB schema from spec §2
    │   └── webapp/
    │       ├── WEB-INF/web.xml                         # session-timeout, error pages stubs
    │       └── index.jsp                               # Redirect target (used in Phase 3)
    └── test/
        └── java/local/promptmark/
            ├── boot/SchemaApplierIT.java               # Testcontainers: schema applies clean
            ├── boot/AdminSeederIT.java                 # Testcontainers: upsert idempotent
            └── DevServerSmokeIT.java                   # Boot Tomcat + GET / → 200
```

### File Responsibilities (one-liners)

- `DevServer` — main(), embedded Tomcat startup, port resolution, calls `SchemaApplier` and `AdminSeeder` after DataSource ready.
- `Env` — loads `.env` file once, falls back to `System.getenv()`, provides typed accessors with defaults.
- `DataSourceProvider` — exposes a single `javax.sql.DataSource` from HikariCP, configured from `Env`.
- `AppException` + subclasses — domain exceptions with HTTP code + user message. Used everywhere in Phase 2+.
- `TraceIdFilter` — first filter in chain. Generates UUID per request, puts in MDC + response header.
- `HelloServlet` — temporary, removed in Phase 2. Verifies tomcat + filter chain work end-to-end.
- `SchemaApplier` — reads `V1__init.sql` from classpath, executes statements via JDBC. Idempotent (uses `IF NOT EXISTS`).
- `AdminSeeder` — reads `ADMIN_EMAIL` + `ADMIN_PWD` from env, INSERT ... ON CONFLICT DO NOTHING.
- `logback.xml` — pattern includes `%X{traceId}`, console + rolling file.
- `V1__init.sql` — verbatim from spec §2.2 (8 tables + indexes + extensions).
- `web.xml` — session timeout 60 min, error-page stubs for 404/500.

---

## Prerequisites (one-time, before Task 1)

You need installed locally:
- **JDK 11+** — verify with `java -version`
- **Gradle 8.x** — verify with `gradle -v`. If absent on Windows: `winget install Gradle.Gradle` or `scoop install gradle`.
- **Docker Desktop** — for Testcontainers integration tests. Verify with `docker ps`.
- **Supabase project + credentials** — only needed at runtime, not for unit tests.

If any are missing, stop and install before proceeding.

---

### Task 1: Gradle scaffold

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` (generated)

- [ ] **Step 1: Write `settings.gradle`**

```groovy
rootProject.name = 'promptmark'
```

- [ ] **Step 2: Write minimal `build.gradle`**

```groovy
plugins {
    id 'application'
    id 'java'
}

group = 'local.promptmark'
version = '0.1.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

def tomcatVersion = '9.0.102'

dependencies {
    // Embedded Tomcat (servlet container + JSP engine)
    implementation "org.apache.tomcat.embed:tomcat-embed-core:${tomcatVersion}"
    implementation "org.apache.tomcat.embed:tomcat-embed-jasper:${tomcatVersion}"

    // JSTL
    implementation 'javax.servlet.jsp.jstl:javax.servlet.jsp.jstl-api:1.2.1'
    implementation('org.glassfish.web:javax.servlet.jsp.jstl:1.2.5') {
        exclude group: 'xalan'
    }

    // JDBC + pool
    implementation 'org.postgresql:postgresql:42.7.4'
    implementation 'com.zaxxer:HikariCP:5.1.0'

    // Security
    implementation 'org.mindrot:jbcrypt:0.4'

    // Logging
    implementation 'org.slf4j:slf4j-api:2.0.13'
    implementation 'ch.qos.logback:logback-classic:1.5.7'

    // (Phase 3+) bundled jars
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    // Tests
    testImplementation platform('org.junit:junit-bom:5.10.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.assertj:assertj-core:3.26.3'
    testImplementation 'org.testcontainers:postgresql:1.20.1'
    testImplementation 'org.testcontainers:junit-jupiter:1.20.1'
}

application {
    mainClass = 'local.promptmark.DevServer'
}

tasks.named('test', Test) {
    useJUnitPlatform()
    exclude '**/*IT.class'                    // unit tests only by default
}

tasks.register('integrationTest', Test) {
    useJUnitPlatform()
    include '**/*IT.class'
    shouldRunAfter test
}

tasks.named('run', JavaExec) {
    standardInput = System.in
    ['port', 'contextPath'].each { key ->
        def value = System.getProperty(key)
        if (value != null) systemProperty key, value
    }
}
```

- [ ] **Step 3: Generate Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.10`
Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`. No errors.

- [ ] **Step 4: Verify build resolves**

Run: `./gradlew tasks --console=plain`
Expected: prints task list including `run`, `test`, `integrationTest`. Last line: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle build.gradle gradlew gradlew.bat gradle/
git commit -m "build: gradle scaffold with tomcat embed, jdbc, logging, jstl"
```

---

### Task 2: webapp skeleton (web.xml + index.jsp)

**Files:**
- Create: `src/main/webapp/WEB-INF/web.xml`
- Create: `src/main/webapp/index.jsp`

- [ ] **Step 1: Write `WEB-INF/web.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                             http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">

  <display-name>promptmark</display-name>

  <session-config>
    <session-timeout>60</session-timeout>
    <cookie-config>
      <http-only>true</http-only>
    </cookie-config>
  </session-config>

  <welcome-file-list>
    <welcome-file>index.jsp</welcome-file>
  </welcome-file-list>

</web-app>
```

- [ ] **Step 2: Write placeholder `index.jsp`**

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>promptmark</title></head>
<body>
<h1>promptmark</h1>
<p>It works. (Phase 1 placeholder)</p>
<p>trace: <code><%= request.getAttribute("traceId") %></code></p>
</body>
</html>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/
git commit -m "feat: webapp skeleton with web.xml and placeholder index.jsp"
```

---

### Task 3: DevServer (Tomcat embed bootstrap)

**Files:**
- Create: `src/main/java/local/promptmark/DevServer.java`

- [ ] **Step 1: Write `DevServer.java`**

```java
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
```

- [ ] **Step 2: Run and verify**

Run: `./gradlew run`
Expected: logs include `promptmark started at http://localhost:8080/promptmark/`.
Open browser to `http://localhost:8080/promptmark/`. Expected: "It works." page.
Press `Ctrl+C` to stop.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/local/promptmark/DevServer.java
git commit -m "feat: embedded tomcat bootstrap with port fallback"
```

---

### Task 4: Logback configuration

**Files:**
- Create: `src/main/resources/logback.xml`

- [ ] **Step 1: Write `logback.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <property name="LOG_DIR" value="logs"/>
  <property name="PATTERN"
            value="%d{HH:mm:ss.SSS} [%thread] %X{traceId:-no-trace} %-5level %logger{36} - %msg%n"/>

  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder><pattern>${PATTERN}</pattern></encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_DIR}/app.log</file>
    <encoder><pattern>${PATTERN}</pattern></encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>${LOG_DIR}/app.%d{yyyy-MM-dd}.log</fileNamePattern>
      <maxHistory>30</maxHistory>
    </rollingPolicy>
  </appender>

  <logger name="local.promptmark" level="DEBUG"/>
  <logger name="org.apache" level="WARN"/>
  <logger name="com.zaxxer.hikari" level="INFO"/>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
  </root>

</configuration>
```

- [ ] **Step 2: Verify logs**

Run: `./gradlew run` then `Ctrl+C`.
Expected: console shows formatted log line `HH:mm:ss.SSS [thread] no-trace INFO  local.promptmark.DevServer - promptmark started...`. File `logs/app.log` is created.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/logback.xml
git commit -m "feat: logback config with traceId MDC and rolling file"
```

---

### Task 5: TraceIdFilter

**Files:**
- Create: `src/main/java/local/promptmark/web/TraceIdFilter.java`
- Modify: `src/main/webapp/WEB-INF/web.xml` (register filter)

- [ ] **Step 1: Write `TraceIdFilter.java`**

```java
package local.promptmark.web;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;

public class TraceIdFilter implements Filter {

    private static final String MDC_KEY = "traceId";
    private static final String HEADER  = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_KEY, traceId);
        req.setAttribute(MDC_KEY, traceId);
        if (res instanceof HttpServletResponse) {
            ((HttpServletResponse) res).setHeader(HEADER, traceId);
        }
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
```

- [ ] **Step 2: Register filter in `web.xml`**

Replace the existing `web.xml` content with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                             http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">

  <display-name>promptmark</display-name>

  <filter>
    <filter-name>traceIdFilter</filter-name>
    <filter-class>local.promptmark.web.TraceIdFilter</filter-class>
  </filter>
  <filter-mapping>
    <filter-name>traceIdFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>

  <session-config>
    <session-timeout>60</session-timeout>
    <cookie-config>
      <http-only>true</http-only>
    </cookie-config>
  </session-config>

  <welcome-file-list>
    <welcome-file>index.jsp</welcome-file>
  </welcome-file-list>

</web-app>
```

- [ ] **Step 3: Run and verify trace id in response**

Run: `./gradlew run` then in another terminal:
`curl -sI http://localhost:8080/promptmark/`
Expected: response includes header like `X-Trace-Id: a1b2c3d4`. Page should print same traceId in `<code>` tag. Stop server with Ctrl+C.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/local/promptmark/web/TraceIdFilter.java src/main/webapp/WEB-INF/web.xml
git commit -m "feat: TraceIdFilter writes UUID to MDC and response header"
```

---

### Task 6: Env loader

**Files:**
- Create: `src/main/java/local/promptmark/config/Env.java`
- Create: `.env.example`
- Test: `src/test/java/local/promptmark/config/EnvTest.java`

- [ ] **Step 1: Write `.env.example`**

```
# Supabase Postgres (Transaction Pooler URI from Supabase dashboard)
DB_URL=jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres
DB_USER=postgres.your-project-ref
DB_PASSWORD=

# LLM (leave LLM_PROVIDER empty to run in rule-fallback mode)
LLM_PROVIDER=
OPENAI_API_KEY=
CLAUDE_API_KEY=
EMBEDDING_MODEL=text-embedding-3-small

# Admin seed
ADMIN_EMAIL=admin@local
ADMIN_PWD=changeme!

# Server
PORT=8080
CONTEXT_PATH=/promptmark
```

- [ ] **Step 2: Write failing test `EnvTest.java`**

```java
package local.promptmark.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvTest {

    @Test
    void parsesKeyValueLinesAndIgnoresCommentsAndBlanks(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve(".env");
        Files.write(f, ("# comment\n"
                      + "DB_URL=jdbc:postgresql://x\n"
                      + "\n"
                      + "ADMIN_EMAIL=a@b.com\n"
                      + "EMPTY=\n").getBytes());

        Env env = Env.fromFile(f);

        assertThat(env.get("DB_URL")).isEqualTo("jdbc:postgresql://x");
        assertThat(env.get("ADMIN_EMAIL")).isEqualTo("a@b.com");
        assertThat(env.get("EMPTY")).isEmpty();
    }

    @Test
    void fallsBackToSystemEnvWhenFileMissing(@TempDir Path tmp) {
        Path missing = tmp.resolve(".env");
        Env env = Env.fromFile(missing);                       // file does not exist
        assertThat(env.get("PATH")).isNotNull();               // system PATH should exist
    }

    @Test
    void getRequiredThrowsWhenMissing(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve(".env");
        Files.write(f, "FOO=bar\n".getBytes());
        Env env = Env.fromFile(f);
        assertThatThrownBy(() -> env.getRequired("MISSING_KEY"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MISSING_KEY");
    }

    @Test
    void getOrDefaultReturnsDefaultWhenMissing(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve(".env");
        Files.write(f, "FOO=bar\n".getBytes());
        Env env = Env.fromFile(f);
        assertThat(env.getOrDefault("FOO", "x")).isEqualTo("bar");
        assertThat(env.getOrDefault("MISSING", "default")).isEqualTo("default");
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew test --tests local.promptmark.config.EnvTest --console=plain`
Expected: FAILURE — `cannot find symbol: class Env` or compile error.

- [ ] **Step 4: Implement `Env.java`**

```java
package local.promptmark.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class Env {

    private final Map<String, String> values;

    private Env(Map<String, String> values) {
        this.values = values;
    }

    /** Load .env at given path (silently ignored if missing) + System.getenv() (lower priority). */
    public static Env fromFile(Path envFile) {
        Map<String, String> map = new HashMap<>(System.getenv());
        if (envFile != null && Files.isRegularFile(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq < 0) continue;
                    String key = trimmed.substring(0, eq).trim();
                    String val = trimmed.substring(eq + 1).trim();
                    map.put(key, val);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read " + envFile, e);
            }
        }
        return new Env(map);
    }

    /** Default loader: looks for ./.env in current working directory. */
    public static Env load() {
        return fromFile(Paths.get(".env"));
    }

    public String get(String key) {
        return values.get(key);
    }

    public String getOrDefault(String key, String def) {
        String v = values.get(key);
        return (v == null) ? def : v;
    }

    public String getRequired(String key) {
        String v = values.get(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("Missing required env var: " + key);
        }
        return v;
    }

    public int getInt(String key, int def) {
        String v = values.get(key);
        if (v == null || v.isEmpty()) return def;
        return Integer.parseInt(v);
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew test --tests local.promptmark.config.EnvTest --console=plain`
Expected: 4 tests passed. `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/local/promptmark/config/Env.java src/test/java/local/promptmark/config/EnvTest.java .env.example
git commit -m "feat: Env loader with .env file + system env fallback"
```

---

### Task 7: AppException hierarchy

**Files:**
- Create: `src/main/java/local/promptmark/web/AppException.java`
- Create: `src/main/java/local/promptmark/web/NotFoundException.java`
- Create: `src/main/java/local/promptmark/web/ForbiddenException.java`
- Create: `src/main/java/local/promptmark/web/UnauthorizedException.java`
- Create: `src/main/java/local/promptmark/web/ValidationException.java`
- Create: `src/main/java/local/promptmark/web/ConflictException.java`
- Test: `src/test/java/local/promptmark/web/AppExceptionTest.java`

- [ ] **Step 1: Write failing test `AppExceptionTest.java`**

```java
package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AppExceptionTest {

    @Test
    void notFoundCarries404() {
        AppException e = new NotFoundException("asset not found");
        assertThat(e.code()).isEqualTo(404);
        assertThat(e.userMessage()).isEqualTo("asset not found");
    }

    @Test
    void forbiddenCarries403() {
        assertThat(new ForbiddenException("nope").code()).isEqualTo(403);
    }

    @Test
    void unauthorizedCarries401() {
        assertThat(new UnauthorizedException("login").code()).isEqualTo(401);
    }

    @Test
    void conflictCarries409() {
        assertThat(new ConflictException("duplicate email").code()).isEqualTo(409);
    }

    @Test
    void validationCarriesFieldErrors() {
        Map<String, String> errors = Map.of("email", "invalid", "password", "too short");
        ValidationException e = new ValidationException("validation failed", errors);
        assertThat(e.code()).isEqualTo(400);
        assertThat(e.fieldErrors()).containsEntry("email", "invalid")
                                   .containsEntry("password", "too short");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests local.promptmark.web.AppExceptionTest --console=plain`
Expected: FAILURE — compile errors `cannot find symbol`.

- [ ] **Step 3: Implement `AppException.java`**

```java
package local.promptmark.web;

public class AppException extends RuntimeException {

    private final int code;
    private final String userMessage;

    public AppException(int code, String userMessage) {
        super(userMessage);
        this.code = code;
        this.userMessage = userMessage;
    }

    public AppException(int code, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.code = code;
        this.userMessage = userMessage;
    }

    public int code() { return code; }
    public String userMessage() { return userMessage; }
}
```

- [ ] **Step 4: Implement the five subclasses**

`NotFoundException.java`:
```java
package local.promptmark.web;

public class NotFoundException extends AppException {
    public NotFoundException(String msg) { super(404, msg); }
}
```

`ForbiddenException.java`:
```java
package local.promptmark.web;

public class ForbiddenException extends AppException {
    public ForbiddenException(String msg) { super(403, msg); }
}
```

`UnauthorizedException.java`:
```java
package local.promptmark.web;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String msg) { super(401, msg); }
}
```

`ConflictException.java`:
```java
package local.promptmark.web;

public class ConflictException extends AppException {
    public ConflictException(String msg) { super(409, msg); }
}
```

`ValidationException.java`:
```java
package local.promptmark.web;

import java.util.Collections;
import java.util.Map;

public class ValidationException extends AppException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String msg, Map<String, String> fieldErrors) {
        super(400, msg);
        this.fieldErrors = (fieldErrors == null)
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(fieldErrors);
    }

    public Map<String, String> fieldErrors() { return fieldErrors; }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew test --tests local.promptmark.web.AppExceptionTest --console=plain`
Expected: 5 tests passed. `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/local/promptmark/web/AppException.java \
        src/main/java/local/promptmark/web/NotFoundException.java \
        src/main/java/local/promptmark/web/ForbiddenException.java \
        src/main/java/local/promptmark/web/UnauthorizedException.java \
        src/main/java/local/promptmark/web/ValidationException.java \
        src/main/java/local/promptmark/web/ConflictException.java \
        src/test/java/local/promptmark/web/AppExceptionTest.java
git commit -m "feat: AppException hierarchy with HTTP code + field errors"
```

---

### Task 8: DataSourceProvider (HikariCP)

**Files:**
- Create: `src/main/java/local/promptmark/config/DataSourceProvider.java`

- [ ] **Step 1: Implement `DataSourceProvider.java`**

```java
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
```

- [ ] **Step 2: Commit**

(No test yet — we test it together with `SchemaApplierIT` in Task 10.)

```bash
git add src/main/java/local/promptmark/config/DataSourceProvider.java
git commit -m "feat: HikariCP DataSourceProvider singleton"
```

---

### Task 9: DB schema SQL file

**Files:**
- Create: `src/main/resources/db/migration/V1__init.sql`

- [ ] **Step 1: Write `V1__init.sql`**

```sql
-- promptmark schema v1
-- Apply once per database. All statements are IF NOT EXISTS / CREATE TABLE so re-runs are safe.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(120) UNIQUE NOT NULL,
  password_hash VARCHAR(120) NOT NULL,
  nickname      VARCHAR(40)  NOT NULL,
  role          VARCHAR(10)  NOT NULL CHECK (role IN ('USER','SELLER','ADMIN')),
  status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE','BANNED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assets (
  id             BIGSERIAL PRIMARY KEY,
  seller_id      BIGINT NOT NULL REFERENCES users(id),
  type           VARCHAR(10) NOT NULL CHECK (type IN ('PROMPT','MD')),
  title          VARCHAR(120) NOT NULL,
  summary        VARCHAR(300) NOT NULL,
  body           TEXT,
  file_key       VARCHAR(200),
  demo_url       VARCHAR(500),
  video_url      VARCHAR(500),
  price          INTEGER NOT NULL DEFAULT 0 CHECK (price >= 0),
  status         VARCHAR(10) NOT NULL DEFAULT 'PUBLIC'
                 CHECK (status IN ('PUBLIC','HIDDEN','DELETED')),
  view_count     INTEGER NOT NULL DEFAULT 0,
  download_count INTEGER NOT NULL DEFAULT 0,
  embedding      vector(1536),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_assets_status_created ON assets(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_assets_seller         ON assets(seller_id);
CREATE INDEX IF NOT EXISTS idx_assets_title_trgm     ON assets USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_assets_embedding
       ON assets USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE IF NOT EXISTS tags (
  id   BIGSERIAL PRIMARY KEY,
  name VARCHAR(40) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS asset_tags (
  asset_id BIGINT REFERENCES assets(id) ON DELETE CASCADE,
  tag_id   BIGINT REFERENCES tags(id)   ON DELETE CASCADE,
  PRIMARY KEY (asset_id, tag_id)
);

CREATE TABLE IF NOT EXISTS orders (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id),
  total_amount INTEGER NOT NULL CHECK (total_amount >= 0),
  status       VARCHAR(10) NOT NULL CHECK (status IN ('PAID','CANCELED')),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_items (
  id         BIGSERIAL PRIMARY KEY,
  order_id   BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  asset_id   BIGINT NOT NULL REFERENCES assets(id),
  price_paid INTEGER NOT NULL CHECK (price_paid >= 0),
  UNIQUE (order_id, asset_id)
);

CREATE TABLE IF NOT EXISTS downloads (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL REFERENCES users(id),
  asset_id      BIGINT NOT NULL REFERENCES assets(id),
  downloaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_downloads_user ON downloads(user_id, downloaded_at DESC);

CREATE TABLE IF NOT EXISTS reports (
  id          BIGSERIAL PRIMARY KEY,
  asset_id    BIGINT NOT NULL REFERENCES assets(id),
  reporter_id BIGINT NOT NULL REFERENCES users(id),
  reason      VARCHAR(300) NOT NULL,
  status      VARCHAR(10) NOT NULL DEFAULT 'OPEN'
              CHECK (status IN ('OPEN','RESOLVED','REJECTED')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/migration/V1__init.sql
git commit -m "feat: V1 init schema (users, assets w/ vector, orders, downloads, reports)"
```

---

### Task 10: SchemaApplier + integration test (Testcontainers)

**Files:**
- Create: `src/main/java/local/promptmark/boot/SchemaApplier.java`
- Test: `src/test/java/local/promptmark/boot/SchemaApplierIT.java`

- [ ] **Step 1: Write failing test `SchemaApplierIT.java`**

```java
package local.promptmark.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaApplierIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("promptmark_test")
        .withUsername("test")
        .withPassword("test");

    private HikariDataSource ds;

    @BeforeAll
    void setUp() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);
    }

    @AfterAll
    void tearDown() {
        if (ds != null) ds.close();
    }

    @Test
    void createsAllTables() throws Exception {
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT count(*) FROM information_schema.tables " +
                "WHERE table_schema='public' AND table_name IN " +
                "('users','assets','tags','asset_tags','orders','order_items','downloads','reports')");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(8);
        }
    }

    @Test
    void isIdempotent() throws Exception {
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");  // second run, must not throw
    }

    @Test
    void vectorColumnExists() throws Exception {
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name='assets' AND column_name='embedding'");
            rs.next();
            assertThat(rs.getString(1)).isEqualToIgnoringCase("USER-DEFINED");  // vector is a custom type
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew integrationTest --tests local.promptmark.boot.SchemaApplierIT --console=plain`
Expected: FAILURE — `cannot find symbol: class SchemaApplier`.

- [ ] **Step 3: Implement `SchemaApplier.java`**

```java
package local.promptmark.boot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchemaApplier {

    private static final Logger log = LoggerFactory.getLogger(SchemaApplier.class);

    private SchemaApplier() {}

    public static void applyFromClasspath(DataSource ds, String classpathResource) {
        String sql = readResource(classpathResource);
        List<String> statements = splitStatements(sql);

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            for (String stmt : statements) {
                if (stmt.isEmpty()) continue;
                log.debug("Executing: {}", stmt.substring(0, Math.min(80, stmt.length())));
                s.execute(stmt);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Schema apply failed: " + e.getMessage(), e);
        }
        log.info("Schema applied: {} statements", statements.size());
    }

    private static String readResource(String path) {
        try (InputStream in = SchemaApplier.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Resource not found: " + path);
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }

    // Splits on ';' that ends a statement. Strips full-line `--` comments only.
    private static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String rawLine : sql.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("--") || line.isEmpty()) continue;
            cur.append(rawLine).append('\n');
            if (rawLine.trim().endsWith(";")) {
                String stmt = cur.toString().trim();
                if (stmt.endsWith(";")) stmt = stmt.substring(0, stmt.length() - 1);
                out.add(stmt.trim());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) {
            String last = cur.toString().trim();
            if (!last.isEmpty()) out.add(last);
        }
        return out;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew integrationTest --tests local.promptmark.boot.SchemaApplierIT --console=plain`
Expected: 3 tests passed. (First run may take ~30s to pull the `pgvector/pgvector:pg16` image.) `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/local/promptmark/boot/SchemaApplier.java \
        src/test/java/local/promptmark/boot/SchemaApplierIT.java
git commit -m "feat: SchemaApplier reads classpath SQL and runs against pgvector container"
```

---

### Task 11: AdminSeeder + integration test

**Files:**
- Create: `src/main/java/local/promptmark/boot/AdminSeeder.java`
- Test: `src/test/java/local/promptmark/boot/AdminSeederIT.java`

- [ ] **Step 1: Write failing test `AdminSeederIT.java`**

```java
package local.promptmark.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mindrot.jbcrypt.BCrypt;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminSeederIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("promptmark_test")
        .withUsername("test")
        .withPassword("test");

    private HikariDataSource ds;

    @BeforeAll
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
    }

    @AfterAll
    void tearDown() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        }
        if (ds != null) ds.close();
    }

    @Test
    void insertsAdminWhenAbsent() throws Exception {
        AdminSeeder.seed(ds, "boss@local", "pwd123!");

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT email, role, password_hash FROM users WHERE email='boss@local'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("email")).isEqualTo("boss@local");
            assertThat(rs.getString("role")).isEqualTo("ADMIN");
            assertThat(BCrypt.checkpw("pwd123!", rs.getString("password_hash"))).isTrue();
        }
    }

    @Test
    void isIdempotent() throws Exception {
        AdminSeeder.seed(ds, "boss2@local", "pwd123!");
        AdminSeeder.seed(ds, "boss2@local", "different_password");  // second call: must not throw

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT count(*) FROM users WHERE email='boss2@local'");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);   // no duplicates
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew integrationTest --tests local.promptmark.boot.AdminSeederIT --console=plain`
Expected: FAILURE — `cannot find symbol: class AdminSeeder`.

- [ ] **Step 3: Implement `AdminSeeder.java`**

```java
package local.promptmark.boot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private AdminSeeder() {}

    public static void seed(DataSource ds, String email, String rawPassword) {
        if (email == null || email.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            log.warn("Skipping admin seed — ADMIN_EMAIL or ADMIN_PWD missing");
            return;
        }
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        String sql = "INSERT INTO users (email, password_hash, nickname, role) " +
                     "VALUES (?, ?, ?, 'ADMIN') " +
                     "ON CONFLICT (email) DO NOTHING";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hash);
            ps.setString(3, "admin");
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("ADMIN seeded: {}", email);
            } else {
                log.info("ADMIN already exists, skipped: {}", email);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Admin seed failed: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew integrationTest --tests local.promptmark.boot.AdminSeederIT --console=plain`
Expected: 2 tests passed. `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/local/promptmark/boot/AdminSeeder.java \
        src/test/java/local/promptmark/boot/AdminSeederIT.java
git commit -m "feat: AdminSeeder upserts ADMIN row idempotently"
```

---

### Task 12: Wire DataSource + schema + seeder into DevServer

**Files:**
- Modify: `src/main/java/local/promptmark/DevServer.java`

- [ ] **Step 1: Replace `DevServer.java` with the wired version**

```java
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
```

- [ ] **Step 2: Create local `.env` (not committed)**

Copy `.env.example` to `.env` and fill in real Supabase credentials.

```bash
cp .env.example .env
# Edit .env with your Supabase DB_URL, DB_USER, DB_PASSWORD
```

- [ ] **Step 3: Run end-to-end**

Run: `./gradlew run`
Expected logs (in order):
- `Schema applied: 14 statements` (or similar count)
- `ADMIN seeded: admin@local` (or `ADMIN already exists, skipped`)
- `promptmark started at http://localhost:8080/promptmark/`

Browse to `http://localhost:8080/promptmark/` → "It works." page with traceId.
`curl -sI http://localhost:8080/promptmark/` → `X-Trace-Id` header present.
Stop with Ctrl+C → log line `promptmark stopped`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/local/promptmark/DevServer.java
git commit -m "feat: wire DataSource + SchemaApplier + AdminSeeder into DevServer startup"
```

---

### Task 13: Boot smoke test (Testcontainers, end-to-end)

**Files:**
- Test: `src/test/java/local/promptmark/DevServerSmokeIT.java`

- [ ] **Step 1: Write the smoke test**

```java
package local.promptmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import local.promptmark.boot.AdminSeeder;
import local.promptmark.boot.SchemaApplier;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DevServerSmokeIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("promptmark_test")
        .withUsername("test")
        .withPassword("test");

    private Tomcat tomcat;
    private HikariDataSource ds;
    private int port;
    private String contextPath = "/promptmark";

    @BeforeAll
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
        AdminSeeder.seed(ds, "admin@local", "pwd123!");

        // Random free port (avoid clash with dev 8080)
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) { port = s.getLocalPort(); }

        tomcat = new Tomcat();
        java.io.File workDir = new java.io.File("build/tomcat-test");
        workDir.mkdirs();
        tomcat.setBaseDir(workDir.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();
        Context ctx = tomcat.addWebapp(contextPath,
            new java.io.File("src/main/webapp").getAbsoluteFile().getAbsolutePath());
        ctx.setReloadable(false);
        tomcat.start();
    }

    @AfterAll
    void tearDown() throws Exception {
        if (tomcat != null) { tomcat.stop(); tomcat.destroy(); }
        if (ds != null) ds.close();
    }

    @Test
    void homePageReturns200WithTraceIdHeader() throws Exception {
        URL url = new URL("http://localhost:" + port + contextPath + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getHeaderField("X-Trace-Id")).isNotEmpty();

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) body.append(line);
            assertThat(body.toString()).contains("It works");
        }
    }

    @Test
    void adminRowExists() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT count(*) FROM users WHERE email='admin@local' AND role='ADMIN'");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
```

- [ ] **Step 2: Run the smoke test**

Run: `./gradlew integrationTest --tests local.promptmark.DevServerSmokeIT --console=plain`
Expected: 2 tests passed. Logs show Tomcat started on random port, GET returned 200 with trace header.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/local/promptmark/DevServerSmokeIT.java
git commit -m "test: boot smoke test (tomcat + schema + seed + GET / → 200 + X-Trace-Id)"
```

---

### Task 14: Update README + push branch

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update `README.md` Setup section**

Replace the existing "## 실행 (예정)" section with:

```markdown
## 빠른 시작 (Phase 1)

### 사전 준비

- JDK 11+
- Docker Desktop (통합 테스트용)
- Supabase 프로젝트 + Postgres 접속 정보

### Supabase 초기 설정 (한 번)

1. Supabase 프로젝트 생성 → Project Settings → Database → Connection String (Transaction Pooler) 복사
2. SQL Editor에서 다음을 한 번 실행:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   CREATE EXTENSION IF NOT EXISTS pg_trgm;
   ```
3. `.env.example`을 `.env`로 복사 후 `DB_URL`, `DB_USER`, `DB_PASSWORD` 채우기

### 실행

```bash
cp .env.example .env       # 그리고 키 채우기
./gradlew run              # http://localhost:8080/promptmark/
./gradlew test             # 단위 테스트
./gradlew integrationTest  # 통합 테스트 (Docker 필요)
```

부팅 시 자동으로:
- DB 스키마 적용 (`V1__init.sql`)
- `ADMIN_EMAIL`/`ADMIN_PWD`로 ADMIN 계정 upsert

### 트레이스 ID

모든 요청에 `X-Trace-Id` 응답 헤더가 붙고, 같은 ID가 로그의 `%X{traceId}`로 찍힙니다.
```

- [ ] **Step 2: Commit README**

```bash
git add README.md
git commit -m "docs: README with Phase 1 quick-start"
```

- [ ] **Step 3: Push all Phase 1 commits**

```bash
git push origin main
```

Expected: push succeeds; new commits visible at https://github.com/b-hyoung/promptmark/commits/main

---

## Done Criteria (Phase 1 complete)

All of these must be true:

1. `./gradlew tasks` succeeds.
2. `./gradlew run` boots, applies schema, seeds ADMIN, and serves `/promptmark/` with traceId header.
3. `./gradlew test` runs unit tests (`EnvTest`, `AppExceptionTest`) — all green.
4. `./gradlew integrationTest` runs `SchemaApplierIT`, `AdminSeederIT`, `DevServerSmokeIT` — all green.
5. Supabase Postgres has 8 tables created (verify in dashboard SQL editor).
6. `users` table contains exactly one row with `role='ADMIN'`.
7. README updated with Phase 1 quick-start.
8. All commits pushed to `main` branch of `b-hyoung/promptmark`.

Next phase: **Phase 2 — Auth**. Building on this foundation: signup/login/logout, BCrypt password storage, session-based auth, CSRF protection, AuthFilter with role-based access control.
