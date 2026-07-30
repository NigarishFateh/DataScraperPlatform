# Annotations Used in This Project

Simple explanations of every annotation used in the Data Scraper Platform.

---

## 1. Spring Boot Startup

### `@SpringBootApplication`
**Where:** Main classes like `ScraperOrchestratorApplication`, `ScraperWebsiteApplication`, etc.

**What it does:** Starts the whole Spring Boot app.

**Easy words:** This is the “power button.” It tells Spring: *scan for components, turn on auto-config, and start the server.*

---

## 2. Web / REST API Annotations

### `@RestController`
**Where:** Controllers (`IntelligenceController`, `HealthController`, scraper controllers, …)

**What it does:** Marks a class as a REST API controller that returns data (usually JSON).

**Easy words:** This class answers HTTP requests and sends JSON back.

---

### `@RequestMapping("/api")`
**Where:** Controllers

**What it does:** Sets a shared URL prefix for all endpoints in that class.

**Easy words:** Every endpoint in this class starts with `/api`.

---

### `@GetMapping`
**Where:** Controllers (`/health`, capability scrape endpoints)

**What it does:** Handles HTTP **GET** requests.

**Easy words:** Used when the client wants to **read** something.

---

### `@PostMapping`
**Where:** `IntelligenceController` (`/jobs`)

**What it does:** Handles HTTP **POST** requests.

**Easy words:** Used when the client wants to **send data** and start an action (like scraping).

---

### `@PathVariable`
**Where:** Scraper controllers (`@PathVariable String category`)

**What it does:** Reads a value from the URL path.

**Easy words:** In `/api/scrape`, the path is the scraper capability endpoint.

---

### `@RequestBody`
**Where:** `IntelligenceController.createJob(...)`

**What it does:** Converts JSON from the request body into a Java object.

**Easy words:** Takes the JSON you send and turns it into `IntelligenceJobRequest`.

---

## 3. Component / Bean Annotations (Dependency Injection)

### `@Service`
**Where:** Service implementations (`IntelligenceOrchestratorServiceImpl`, scraper service impls)

**What it does:** Marks a class as business-logic code and registers it as a Spring bean.

**Easy words:** “This class contains the main work/logic.” Spring creates and manages it.

---

### `@Component`
**Where:** Clients and adapters (`ScraperServiceClientImpl`, remote scrapers, …)

**What it does:** Registers a class as a Spring-managed bean.

**Easy words:** Spring will create this object and inject it where needed.

---

### `@Configuration`
**Where:** Config classes (`WebClientConfig`, `CorsConfig`, `ScraperExecutorConfig`, …)

**What it does:** Marks a class that defines setup/beans for the app.

**Easy words:** “This class configures parts of the application.”

---

### `@Bean`
**Where:** Config methods (`webClient()`, `scraperExecutor()`, CORS configurer)

**What it does:** Creates an object and puts it into Spring’s container.

**Easy words:** You manually build an object, and Spring remembers/reuses it.

---

### `@Qualifier("scraperExecutor")`
**Where:** `IntelligenceOrchestratorServiceImpl` constructor

**What it does:** Chooses a specific bean when more than one of the same type exists.

**Easy words:** “Don’t inject any Executor — inject the one named `scraperExecutor`.”

---

## 4. Configuration from YAML

### `@ConfigurationProperties`
**Where:** Properties records (`IntelligenceScraperProperties`, `ScraperResilienceProperties`, …)

**What it does:** Maps values from `application.yml` into a Java object.

**Easy words:** Reads settings from YAML and fills this class automatically.

---

### `@EnableConfigurationProperties`
**Where:** Config classes (`WebClientConfig`, scraper `ScraperConfig`)

**What it does:** Turns on `@ConfigurationProperties` classes so Spring can create them.

**Easy words:** “Please activate these settings classes.”

---

### `@Value("${...}")`
**Where:** Scraper service constructors (timeouts)

**What it does:** Injects one config value from YAML/properties.

**Easy words:** Pull one setting (like timeout) into a constructor parameter.

---

## 5. Lombok Annotations

### `@RequiredArgsConstructor`
**Where:** Controllers

**What it does:** Generates a constructor for all `final` fields.

**Easy words:** Saves typing constructors. Used for constructor-based dependency injection.

---

### `@Slf4j`
**Where:** Services and clients

**What it does:** Creates a `log` field for logging.

**Easy words:** Gives you `log.info(...)` without writing logger setup code.

---

## 6. Validation Annotations

### `@Valid`
**Where:** Intelligence job endpoint

**What it does:** Tells Spring to validate the incoming request object.

**Easy words:** Check the request before running job logic.

---

### `@NotEmpty`
**Where:** `IntelligenceJobRequest` fields (e.g. `scraperTypes`)

**What it does:** Requires a list to be present and not empty.

**Easy words:** You must select at least one scraper type.

---

## 7. Error Handling Annotations

### `@RestControllerAdvice`
**Where:** `GlobalExceptionHandler` classes

**What it does:** Central place to catch exceptions from controllers and return clean API errors.

**Easy words:** Global “error translator” for HTTP responses.

---

### `@ExceptionHandler`
**Where:** Methods inside `GlobalExceptionHandler`

**What it does:** Handles one specific exception type.

**Easy words:** “If this error happens, return this JSON response.”

---

## 8. JSON Annotations

### `@JsonCreator`
**Where:** Enums like `DataCategory`, `ScraperSource`

**What it does:** Tells Jackson how to create the enum from JSON text.

**Easy words:** Lets JSON values like `"jobs"` become the Java enum `JOBS`.

---

## 9. Java / Testing Annotations

### `@Override`
**Where:** Service implementations and interface methods

**What it does:** Marks that a method overrides a parent/interface method.

**Easy words:** Compiler check: “Yes, this method replaces the parent one.”

---

### `@SpringBootTest`
**Where:** Test classes (`*ApplicationTests`)

**What it does:** Loads the full Spring Boot application context for testing.

**Easy words:** Starts the app in test mode to verify it boots correctly.

---

### `@Test`
**Where:** Test methods (`contextLoads`)

**What it does:** Marks a method as a unit/integration test.

**Easy words:** “Run this method as a test.”

---

## Quick Cheat Sheet

| Annotation | One-line meaning |
|---|---|
| `@SpringBootApplication` | Start the app |
| `@RestController` | JSON API class |
| `@RequestMapping` | Shared URL prefix |
| `@GetMapping` | Handle GET |
| `@PostMapping` | Handle POST |
| `@PathVariable` | Read URL path value |
| `@RequestBody` | Read JSON body |
| `@Service` | Business logic bean |
| `@Component` | Generic Spring bean |
| `@Configuration` | Setup/config class |
| `@Bean` | Manually create a bean |
| `@Qualifier` | Pick a specific bean |
| `@ConfigurationProperties` | Map YAML → object |
| `@EnableConfigurationProperties` | Activate properties classes |
| `@Value` | Inject one config value |
| `@RequiredArgsConstructor` | Auto constructor (Lombok) |
| `@Slf4j` | Auto logger (Lombok) |
| `@Valid` | Validate request |
| `@NotEmpty` | List must not be empty |
| `@RestControllerAdvice` | Global exception handler |
| `@ExceptionHandler` | Handle one exception type |
| `@JsonCreator` | Create object/enum from JSON |
| `@Override` | Method overrides parent |
| `@SpringBootTest` | Boot full app for tests |
| `@Test` | Mark test method |

---

## Mental Model (Beginner)

Think of annotations as **sticky notes** for Spring:

1. You put a sticky note on a class (`@Service`, `@RestController`, …)
2. Spring reads the sticky notes at startup
3. Spring creates objects (beans) and wires them together
4. When a request arrives, Spring knows which method should handle it

That is dependency injection + request routing in simple words.
