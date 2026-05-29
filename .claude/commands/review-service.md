# /review-service — Microservice Consistency Review

Review the microservice named in `$ARGUMENTS` for consistency with the established patterns in this project.
If no argument is given, ask the user which service to review (options: user, catalog, inventory, ordering, shipment, analytics, gateway).

The project root is `C:\Users\kbosnjak3\IdeaProjects\katarina-learning`.
The service directory is `<root>\$ARGUMENTS`.
Base package: `hr.fer.dipl`

---

## Step 1 — Collect all source files

Read every `.java` file and `application.yaml` / `application.yml` in `<service>\src\main\java\` and `<service>\src\main\resources\`.
Build a mental map of what exists before you start reviewing.

---

## Step 2 — Security layer

**File to check:** `config/WebSecurityConfiguration.java`

Verify all of the following:
- [ ] Class is annotated `@Configuration` + `@EnableWebSecurity`
- [ ] Uses `@RequiredArgsConstructor` (no field `@Autowired`)
- [ ] `filterChain` disables CSRF and CORS: `.csrf(AbstractHttpConfigurer::disable)` and `.cors(AbstractHttpConfigurer::disable)`
- [ ] Session management is STATELESS: `SessionCreationPolicy.STATELESS`
- [ ] `JwtAuthenticationFilter` is added **before** `UsernamePasswordAuthenticationFilter`
- [ ] Public endpoints are explicitly listed with `.permitAll()` (not left as `.anyRequest().permitAll()`)
- [ ] `PasswordEncoder` bean is only present in services that actually hash passwords (user service)

**File to check:** `config/JwtAuthenticationFilter.java`

Verify:
- [ ] Extends `OncePerRequestFilter`
- [ ] Implements the **two-layer auth pattern**:
  - Layer 1: if JWT present → validate with `JwtService`, set SecurityContext from claims
  - Layer 2: if no JWT → trust `X-User-Id` / `X-User-Role` headers (gateway path)
- [ ] Invalid JWT does **not** fall through to Layer 2 (security boundary)
- [ ] Token resolution checks both `Authorization: Bearer` header AND `token` cookie
- [ ] `UsernamePasswordAuthenticationToken` principal is `Long userId` (not a username string)

Flag any deviation from the above as a ⚠️ finding.

---

## Step 3 — Exception handling

**File to check:** `exception/GlobalExceptionHandler.java`

Verify:
- [ ] Annotated `@RestControllerAdvice` (not just `@ControllerAdvice` — that won't serialise to JSON by default)
- [ ] Has a handler for the **service-specific** custom exception (e.g. `InventoryException`, `OrderException`)
- [ ] Has a fallback `@ExceptionHandler(Exception.class)` returning 500
- [ ] Validation errors (`MethodArgumentNotValidException`) are handled and return 400 with field-level details
- [ ] `DataIntegrityViolationException` is handled and returns 409 (if the service writes to a DB)
- [ ] Error response body is consistent: includes at minimum `status` (int), `message` (String), `timestamp` (long)

Flag if the service is missing any domain-specific exception class — it likely means exceptions bubble up as generic 500s.

---

## Step 4 — Feign clients (only if service calls other services)

Look for any `@FeignClient` interfaces under `client/`.

For each one verify:
- [ ] URL uses property placeholder with fallback: `url = "${some-service.url:http://localhost:PORT}"` — **not** a hardcoded URL
- [ ] The matching property exists in `application.yaml`
- [ ] The service has a `FeignClientInterceptor` that forwards `Authorization: Bearer <token>` from the SecurityContext
- [ ] `FeignClientInterceptor.apply()` casts auth details to get the raw token — verify the cast matches how the token is stored (currently `UsernamePasswordAuthenticationToken.getDetails()`)
- [ ] `@EnableFeignClients` is present on the main `Application` class or a config class

---

## Step 5 — Controllers

For each `@RestController`:
- [ ] Uses constructor injection (`@RequiredArgsConstructor` or explicit constructor) — **no `@Autowired` on fields**
- [ ] `@RequestMapping` has a meaningful path prefix (not left blank like `@RequestMapping` with no value)
- [ ] Admin-only endpoints are protected with `@PreAuthorize("hasRole('ADMIN')")`, not left open
- [ ] Create endpoints return `HttpStatus.CREATED` (201), not 200
- [ ] Delete endpoints return 204 No Content, not 200 with a body
- [ ] `@PathVariable` and `@RequestParam` names match the path template exactly
- [ ] No business logic in the controller — all logic delegated to a service

---

## Step 6 — Service layer

- [ ] Service is defined as an **interface** with a separate `*ServiceImpl` class (established after commit `4710f2e`)
- [ ] If a service that existed before that refactor is still not split, flag it

---

## Step 7 — application.yaml

Verify:
- [ ] `server.port` is set and matches the gateway route table (gateway `application.yml`)
- [ ] `spring.jpa.hibernate.ddl-auto` is `validate` (never `create` or `update` in any committed config)
- [ ] `spring.liquibase.enabled: true` with correct schema names if it's a DB-backed service
- [ ] `security.jwt.public-key-path: classpath:public.pem` is present
- [ ] `spring.kafka.bootstrap-servers: localhost:19093` is present if the service uses Kafka

---

## Step 8 — Output

Produce a structured review report with the following sections:

```
## Review: <service-name>

### ✅ Passing checks
(list what looks good)

### ⚠️ Findings
For each finding:
- **[SEVERITY: HIGH/MEDIUM/LOW]** Description of the problem
  - File: `path/to/file.java:line`
  - Why it matters: ...
  - Suggested fix: ...

### 📋 Summary
X checks passed, Y findings (Z high, W medium, V low)
```

Be specific — cite exact file paths and line numbers for every finding.
