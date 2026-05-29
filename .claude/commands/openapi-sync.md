# /openapi-sync — Check OpenAPI Spec vs. Actual Controller Endpoints

Compare the `openapi.yaml` of a service against its actual `@RestController` endpoints and report any drift.

**Argument:** `$ARGUMENTS` — service name (e.g. `catalog`). If omitted, ask.

Project root: `C:\Users\kbosnjak3\IdeaProjects\katarina-learning`

---

## Step 1 — Read all relevant files

1. Read `<service>/openapi.yaml` (may be empty or minimal — note that too)
2. Read every `*Controller.java` under `<service>/src/main/java/hr/fer/dipl/controller/`
3. Note the `@RequestMapping` base path on each controller class
4. Note every `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` method and its path

---

## Step 2 — Extract the "truth" from controllers

Build a table of every endpoint actually implemented:

| Method | Full path | Returns | Auth required | Notes |
|--------|-----------|---------|---------------|-------|
| GET    | /products/{productId} | ProductDTO | no | |
| GET    | /products/search | Page<ProductDTO> | no | query param: q, page, size |
| ...    | ...       | ...     | ...           | |

Derive the full path by combining the class-level `@RequestMapping` value with the method-level mapping value.

---

## Step 3 — Extract what the openapi.yaml documents

If the file is empty or has no `paths:` section, note: **"openapi.yaml has no documented paths"** and skip to Step 5.

Otherwise, extract the documented paths and methods in the same table format.

---

## Step 4 — Compare and find drift

Check three categories of drift:

**A. Endpoints in controller but NOT in openapi.yaml (undocumented)**
- These work but clients don't know about them.

**B. Endpoints in openapi.yaml but NOT in controller (ghost docs)**
- These are documented but don't exist — a client calling them gets 404.

**C. Documented but wrong (signature mismatch)**
For matching path+method pairs, check:
- Response type matches (e.g. spec says array, controller returns Page)
- Path parameters match (e.g. `{id}` vs `{productId}`)
- Required query params documented
- Request body presence matches (`@RequestBody` in controller ↔ `requestBody` in spec)
- HTTP status codes documented match what the controller actually returns

---

## Step 5 — Generate missing spec entries

For every undocumented endpoint (category A), generate the correct OpenAPI YAML snippet:

```yaml
/products/{productId}:
  get:
    summary: Get product by ID
    parameters:
      - name: productId
        in: path
        required: true
        schema:
          type: integer
    responses:
      '200':
        description: Product found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductDTO'
      '404':
        description: Product not found
```

Base the schema references on the actual DTO field names from the Java classes.

---

## Step 6 — Output report

```
## OpenAPI Sync Report: <service>

### 📊 Summary
- Controller endpoints found:   <N>
- OpenAPI paths documented:     <M>
- ✅ In sync:                   <X>
- ⚠️  Undocumented (A):         <Y>
- 🚫 Ghost docs (B):            <Z>
- 🔀 Signature mismatch (C):    <W>

---

### ⚠️ Undocumented endpoints (exist in code, missing from spec)
(list each one with the generated YAML snippet to add)

### 🚫 Ghost documentation (in spec but no matching controller method)
(list each one)

### 🔀 Signature mismatches
(list each one with: what spec says vs. what controller does)

---

### 🛠️ Suggested action
- If openapi.yaml is empty or incomplete: offer to write the full spec from scratch based on all controllers
- If there are ghost docs: offer to remove them
- If there are mismatches: show exact diff and offer to fix the spec
```

After showing the report, ask: **"Would you like me to update `openapi.yaml` with the correct entries?"**
If yes, write the corrected file.
