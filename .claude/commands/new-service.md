# /new-service — Scaffold a New Spring Boot Microservice

Scaffold a new microservice that matches every established pattern in this project.

**Argument:** `$ARGUMENTS` — the name of the new service in kebab-case (e.g. `payment`, `notification`).
If no argument is given, ask for the service name before doing anything.

Project root: `C:\Users\kbosnjak3\IdeaProjects\katarina-learning`
Base package: `hr.fer.dipl`

---

## Before you start — ask these questions

Ask the user in one message (don't proceed until answered):
1. **Port** — which port should this service run on? (Check existing: user=8080, catalog=8081, shipment=8082, inventory=8083, ordering=8084, analytics=8086, gateway=8090)
2. **Database** — does this service have its own PostgreSQL schema? (yes/no)
3. **Kafka** — does this service produce or consume Kafka events? (producer / consumer / both / no)
4. **Feign clients** — does it call any existing service? If yes, which ones?
5. **Domain entity** — what is the main entity name? (e.g. `Payment`, `Notification`)

---

## Step 1 — Directory structure

Create the following layout (replace `<service>` with the argument, `<Entity>` with the entity name):

```
<service>/
  gradlew                     ← copy from catalog/gradlew
  gradlew.bat                 ← copy from catalog/gradlew.bat
  settings.gradle.kts
  build.gradle.kts
  src/
    main/
      java/hr/fer/dipl/
        <Entity>Application.java
        controller/
          <Entity>Controller.java
        service/
          <Entity>Service.java          ← interface
          impl/
            <Entity>ServiceImpl.java
        db/
          model/
            <Entity>.java
          repository/
            <Entity>Repository.java
        dto/
          <Entity>DTO.java
        exception/
          GlobalExceptionHandler.java
          <Entity>Exception.java
        config/
          WebSecurityConfiguration.java
          JwtAuthenticationFilter.java  ← copy exact pattern
        mapper/
          <Entity>Mapper.java
        [client/  ← only if Feign clients needed]
          FeignClientInterceptor.java
          <other-service>/
            <OtherService>Client.java
      resources/
        application.yaml
        public.pem                      ← copy from catalog/src/main/resources/public.pem
        [db/changelog/  ← only if DB]
          db.changelog-master.yaml
          01-create-<entity>-table.xml
    test/
      java/hr/fer/dipl/
        controller/
          <Entity>ControllerTest.java   ← stub, tell user to run /gen-tests after
```

---

## Step 2 — settings.gradle.kts

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "<service>"
```

---

## Step 3 — build.gradle.kts

Base dependencies (always included):
```kotlin
plugins {
    id("java")
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "hr.fer.dipl"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.1")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
}

tasks.test {
    useJUnitPlatform()
}
```

Add conditionally:
- If **DB**: add `spring-boot-starter-data-jpa`, `postgresql`, `liquibase-core`
- If **Kafka**: add `spring-kafka`
- If **Feign clients**: add `spring-cloud-starter-openfeign`

---

## Step 4 — application.yaml

```yaml
server:
  port: <PORT>

spring:
  application:
    name: <service>-service
  # include only if DB:
  datasource:
    url: jdbc:postgresql://localhost:45432/<service>
    username: <service>
    password: changeme
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: <service>
    liquibase-schema: <service>
    database-change-log-table: databasechangelog
    database-change-log-lock-table: databasechangeloglock
  # include only if Kafka:
  kafka:
    bootstrap-servers: localhost:19093
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: <service>-group
      properties:
        spring.json.trusted.packages: "*"

security:
  jwt:
    public-key-path: classpath:public.pem

# include one line per Feign client:
# <other>-service:
#   url: "http://localhost:<PORT>"
```

---

## Step 5 — Application main class

```java
package hr.fer.dipl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// add @EnableFeignClients if Feign is used

@SpringBootApplication
public class <Entity>Application {
    public static void main(String[] args) {
        SpringApplication.run(<Entity>Application.class, args);
    }
}
```

---

## Step 6 — Security config files

Copy the **exact** `WebSecurityConfiguration.java` and `JwtAuthenticationFilter.java` patterns from `inventory/src/main/java/hr/fer/dipl/config/`.

Adjust `WebSecurityConfiguration.filterChain()`:
- Update `.requestMatchers(...)` to use the new service's public paths (if any)
- If no public paths, use `.anyRequest().authenticated()`

The `JwtAuthenticationFilter` should be copied **verbatim** — it is intentionally identical across services (two-layer auth pattern).

---

## Step 7 — JwtService

Copy `JwtService` from `inventory/src/main/java/hr/fer/dipl/service/JwtService.java` verbatim. It reads `public.pem` from classpath and is shared across all services.

---

## Step 8 — GlobalExceptionHandler

```java
package hr.fer.dipl.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(<Entity>Exception.class)
    public ResponseEntity<ErrorResponse> handle<Entity>Exception(<Entity>Exception ex) {
        return new ResponseEntity<>(
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), System.currentTimeMillis()),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce("", (a, b) -> a + "; " + b);
        return new ResponseEntity<>(
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), msg, System.currentTimeMillis()),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return new ResponseEntity<>(
            new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), System.currentTimeMillis()),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    public record ErrorResponse(int status, String message, long timestamp) {}
}
```

---

## Step 9 — Entity, Repository, DTO, Service interface + impl, Controller

Generate stubs appropriate for the entity name. Use:
- `@Entity` + `@Table` + Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder` for the model
- `JpaRepository<Entity, Long>` for the repository
- Plain record or Lombok `@Data` class for the DTO
- Service interface with at minimum: `getById`, `getAll`, `create`, `update`, `delete`
- Controller returning `ResponseEntity<>` with correct HTTP status codes (201 for create, 204 for delete)

---

## Step 10 — Liquibase changelog (only if DB)

Create `db/changelog/db.changelog-master.yaml`:
```yaml
databaseChangeLog:
  - include:
      file: db/changelog/01-create-<entity>-table.xml
      relativeToChangelogFile: true
```

Create `db/changelog/01-create-<entity>-table.xml` with a `createTable` changeset for the entity's fields.

---

## Step 11 — Gateway route (remind the user)

After scaffolding, print this reminder:

```
⚠️  Don't forget to add a route to gateway/src/main/resources/application.yml:

  - id: <service>-service
    uri: http://localhost:<PORT>
    predicates:
      - Path=/<entity-plural>/**

Also add the DB user to infra/db/create-dbs.sh if a new schema is needed.
```

---

## Step 12 — Final checklist

Print a checklist of what was created and what the user must do manually:
- [ ] Add to gateway routes
- [ ] Add DB user/schema to `infra/db/create-dbs.sh` (if DB service)
- [ ] Copy `private.pem` if this service issues JWTs (only user service needs it — confirm)
- [ ] Run `/gen-tests <service>` to generate test coverage
- [ ] Run `/review-service <service>` after first implementation pass
