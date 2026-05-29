# /gen-tests — Generate JUnit 5 + MockMvc Tests for a Controller

Generate a complete test class for the controller in the service named in `$ARGUMENTS`.
Format: `<service>` or `<service>/<ControllerName>` (e.g. `inventory` or `ordering/OrderController`).

If no argument is given, ask which service and which controller.

Project root: `C:\Users\kbosnjak3\IdeaProjects\katarina-learning`
Base package: `hr.fer.dipl`

---

## Step 1 — Read source files

Read all of the following before writing a single line of test code:

1. The controller file(s) in `<service>/src/main/java/hr/fer/dipl/controller/`
2. The service **interface** (not impl) for each injected service
3. All DTOs used as request/response bodies
4. The entity model (to understand field names and constraints)
5. `exception/GlobalExceptionHandler.java` (to know what error shapes to assert)
6. `config/WebSecurityConfiguration.java` (to know which endpoints are public vs. authenticated)

---

## Step 2 — Determine test strategy per endpoint

For each `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`:

1. **What HTTP status is the happy path?** (200, 201, 204 — check the controller return)
2. **Is the endpoint public or requires auth?** (check `WebSecurityConfiguration.filterChain()`)
3. **What can go wrong?** (entity not found → 404, invalid input → 400, unauthorized → 401/403)
4. **Does it have `@PreAuthorize("hasRole('ADMIN')")`?** If yes, add a 403 test for regular users.

---

## Step 3 — Write the test class

Place the file at:
`<service>/src/test/java/hr/fer/dipl/controller/<ControllerName>Test.java`

Use `@WebMvcTest(<ControllerName>.class)` — NOT `@SpringBootTest` (faster, no DB needed).

### Class structure

```java
package hr.fer.dipl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.fer.dipl.service.<Entity>Service;
// ... other imports

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(<ControllerName>.class)
@AutoConfigureMockMvc(addFilters = false)   // disable security filter for unit tests
class <ControllerName>Test {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean <Entity>Service <entity>Service;
    // add @MockBean for each injected dependency in the controller

    // --- tests below ---
}
```

> **Note on `addFilters = false`**: This disables the JWT filter so tests focus on controller logic.
> Security integration is covered by the two-layer filter design — don't re-test JWT parsing here.

---

## Step 4 — Test naming convention

Use: `should_<expectedBehaviour>_when_<condition>()`

Examples:
- `should_returnProduct_when_productExists()`
- `should_return404_when_productNotFound()`
- `should_return400_when_requestBodyInvalid()`
- `should_return403_when_userIsNotAdmin()`
- `should_return201_when_orderCreated()`

---

## Step 5 — Test templates to generate for each endpoint

### GET by ID
```java
@Test
void should_return200_when_<entity>Exists() throws Exception {
    var dto = // construct a sample DTO with realistic field values
    given(<entity>Service.get<Entity>ById(1L)).willReturn(dto);

    mockMvc.perform(get("/<entities>/1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1));
}

@Test
void should_return404_when_<entity>NotFound() throws Exception {
    given(<entity>Service.get<Entity>ById(99L))
        .willThrow(new <Entity>Exception("Not found"));

    mockMvc.perform(get("/<entities>/99"))
           .andExpect(status().isNotFound());  // or 400 depending on the handler
}
```

### GET all / paginated
```java
@Test
@WithMockUser(roles = "ADMIN")   // add this if endpoint is admin-only
void should_returnPage_when_listRequested() throws Exception {
    given(<entity>Service.getAll(any())).willReturn(Page.empty());

    mockMvc.perform(get("/<entities>"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content").isArray());
}
```

### POST (create)
```java
@Test
void should_return201_when_<entity>Created() throws Exception {
    var request = // build a valid request DTO
    var response = // build expected response DTO
    given(<entity>Service.create<Entity>(any())).willReturn(response);

    mockMvc.perform(post("/<entities>")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists());
}

@Test
void should_return400_when_requestBodyMissingRequiredField() throws Exception {
    var invalid = // DTO with a required field set to null or blank

    mockMvc.perform(post("/<entities>")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(invalid)))
           .andExpect(status().isBadRequest());
}
```

### PUT / PATCH (update)
```java
@Test
void should_return200_when_<entity>Updated() throws Exception {
    var request = // update request DTO
    var updated = // expected updated DTO
    given(<entity>Service.update<Entity>(eq(1L), any())).willReturn(updated);

    mockMvc.perform(put("/<entities>/1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk());
}
```

### DELETE
```java
@Test
void should_return204_when_<entity>Deleted() throws Exception {
    willDoNothing().given(<entity>Service).delete<Entity>(1L);

    mockMvc.perform(delete("/<entities>/1"))
           .andExpect(status().isNoContent());
}
```

### Admin-only endpoint (403 test)
```java
@Test
@WithMockUser(roles = "USER")
void should_return403_when_regularUserAccessesAdminEndpoint() throws Exception {
    mockMvc.perform(get("/<entities>"))
           .andExpect(status().isForbidden());
}
```

---

## Step 6 — Write the file

Write the complete, compilable test class to the correct path. Do **not** leave placeholder comments — fill in realistic DTO values based on the actual field names you read in Step 1.

After writing, print:
```
✅ Generated: <service>/src/test/java/hr/fer/dipl/controller/<ControllerName>Test.java
   Covers: <N> endpoints, <M> test cases
   Run with: cd <service> && .\gradlew.bat test
```

If the service has multiple controllers (e.g. ordering has CartController + OrderController), generate a test class for each one.
