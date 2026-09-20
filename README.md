# ExpensC

ExpensC is a Java 21 and Spring Boot expense management application. It provides a JWT-secured REST API for user authentication, expense management, filtering, and analytics, plus a plain HTML/CSS/vanilla JavaScript frontend served by Spring Boot.

## Key Features

- User registration and login with BCrypt password hashing.
- Stateless JWT authentication using the `Authorization: Bearer <token>` header.
- Per-user expense ownership isolation.
- Expense create, read, update, and delete operations.
- Filtering by category, payment method, date range, amount range, and text search.
- Pagination and configurable sorting for expense lists.
- Overall, category, and monthly expense summaries.
- MySQL persistence through Spring Data JPA and Hibernate.
- Centralized JSON error responses and request validation.
- Swagger UI and OpenAPI JSON documentation.
- Browser dashboard served from the Spring Boot application.
- Spring Boot integration tests using an in-memory H2 database.

## Technology Stack

- Java 21
- Spring Boot 3.3.3
- Maven
- Spring Web
- Spring Data JPA and Hibernate
- Spring Security
- JJWT 0.11.5
- MySQL Connector/J
- springdoc OpenAPI UI 2.6.0
- Jakarta Bean Validation
- Lombok
- JUnit, Spring Boot Test, MockMvc, and H2 for tests
- HTML, CSS, and vanilla JavaScript for the frontend

## Architecture

The application follows a layered Spring architecture:

- `controller`: REST request mappings for authentication and expenses.
- `service`: authentication, current-user lookup, expense CRUD, filtering, and analytics.
- `repository`: Spring Data JPA repositories and dynamic filtering specifications.
- `domain`: JPA entities and enums.
- `dto`: request, response, and error payload records.
- `security`: JWT creation and validation, request filtering, and user lookup.
- `config`: Spring Security, JWT properties, and OpenAPI configuration.
- `exception`: application exceptions and centralized REST exception handling.
- `src/main/resources/static`: the browser frontend served by Spring Boot.

## Authentication and JWT Flow

1. Register with `POST /api/auth/register` or log in with `POST /api/auth/login`.
2. The application authenticates credentials through Spring Security and returns an `AuthResponse` containing `token`, `tokenType`, `email`, and `name`.
3. The JWT subject is the user's email. The token includes issued-at and expiration timestamps.
4. Send the token on protected requests:

   ```http
   Authorization: Bearer <jwt>
   ```

5. `JwtAuthenticationFilter` validates the token, loads the user by email, and populates the Spring Security context.
6. The application uses stateless sessions, BCrypt password hashing, and a disabled CSRF requirement for this API.

Registration trims the name and email, rejects duplicate email addresses, stores the password as a BCrypt hash, and returns a token after successful registration. Authentication endpoints and the static frontend are public; all other API requests require authentication.

## Expense Management

An expense contains:

- `amount`: required and at least `0.01`.
- `description`: required, nonblank, and at most 255 characters.
- `category`: required enum value.
- `paymentMethod`: required enum value.
- `expenseDate`: required ISO date (`YYYY-MM-DD`).

Available categories:

`FOOD`, `TRANSPORT`, `SHOPPING`, `ENTERTAINMENT`, `BILLS`, `HEALTH`, `EDUCATION`, `OTHER`

Available payment methods:

`CASH`, `UPI`, `CREDIT_CARD`, `DEBIT_CARD`, `BANK_TRANSFER`, `OTHER`

Every expense is associated with its owner. List, detail, update, and delete operations only access expenses belonging to the authenticated user. An expense belonging to another user is not exposed by the detail endpoint.

## Filtering and Analytics

`GET /api/expenses` supports these optional query parameters:

- `category`: an `ExpenseCategory` value.
- `paymentMethod`: a `PaymentMethod` value.
- `from` and `to`: inclusive ISO dates.
- `minAmount` and `maxAmount`: inclusive amount bounds.
- `search`: case-insensitive match against description, category, or payment method.
- `page`: zero-based page number; defaults to `0`.
- `size`: page size; defaults to `10`. Values outside `1` through `100` are reset to `10`.
- `sort`: `property,direction`; defaults to `expenseDate,desc`.

Analytics are calculated for the authenticated user's expenses:

- Overall summary: total, average rounded to two decimal places, highest expense, and count.
- Category summary: totals keyed by category name.
- Monthly summary: totals keyed by `YYYY-MM`.

## API Endpoints

All expense and analytics endpoints require `Authorization: Bearer <jwt>`. Authentication, static frontend, and OpenAPI routes are publicly accessible.

| Method | Path | Description | Success response |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Register a user and issue a JWT | `201 Created` with `AuthResponse` |
| `POST` | `/api/auth/login` | Authenticate a user and issue a JWT | `200 OK` with `AuthResponse` |
| `POST` | `/api/expenses` | Create an expense for the current user | `201 Created` with `ExpenseResponse` |
| `GET` | `/api/expenses` | List the current user's expenses with filters and pagination | `200 OK` with `Page<ExpenseResponse>` |
| `GET` | `/api/expenses/{id}` | Get an owned expense | `200 OK` with `ExpenseResponse` |
| `PUT` | `/api/expenses/{id}` | Update an owned expense | `200 OK` with `ExpenseResponse` |
| `DELETE` | `/api/expenses/{id}` | Delete an owned expense | `204 No Content` |
| `GET` | `/api/expenses/summary` | Get overall expense metrics | `200 OK` with summary fields |
| `GET` | `/api/expenses/summary/category` | Get totals grouped by category | `200 OK` with a category-to-amount map |
| `GET` | `/api/expenses/summary/monthly` | Get totals grouped by month | `200 OK` with a month-to-amount map |

Validation and application errors are returned as `ApiErrorResponse` JSON objects. Common statuses include `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, and `409 Conflict` for duplicate email registration.

## Example API Usage

Register:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"password123"}'
```

Login and save the returned `token`:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123"}'
```

Create an expense:

```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"amount":450.00,"description":"Dinner","category":"FOOD","paymentMethod":"UPI","expenseDate":"2026-09-20"}'
```

Filter and paginate expenses:

```bash
curl "http://localhost:8080/api/expenses?category=FOOD&from=2026-09-01&to=2026-09-30&page=0&size=10&sort=expenseDate,desc" \
  -H "Authorization: Bearer <jwt>"
```

## Database Setup

The default application profile uses MySQL with these default settings:

- URL: `jdbc:mysql://localhost:3306/expensetrack?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC`
- Username: `root`
- Password: `password`
- Hibernate DDL mode: `update`
- Server port: `8080`

Install and start MySQL, then provide credentials through environment variables or adjust the defaults in your local environment. The JDBC URL requests creation of the `expensetrack` database when the MySQL user has permission to do so.

Tests do not require MySQL. The test profile uses an in-memory H2 database in MySQL compatibility mode and `create-drop` schema generation.

## Environment Variables and Configuration

The values below override the defaults in `src/main/resources/application.properties`:

| Variable | Purpose | Default |
| --- | --- | --- |
| `EXPENSETRACK_DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/expensetrack?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC` |
| `EXPENSETRACK_DB_USERNAME` | Database username | `root` |
| `EXPENSETRACK_DB_PASSWORD` | Database password | `password` |
| `EXPENSETRACK_JPA_DDL_AUTO` | Hibernate schema mode | `update` |
| `EXPENSETRACK_JWT_SECRET` | JWT signing secret input | development fallback in `application.properties` |
| `EXPENSETRACK_JWT_EXPIRATION` | Token lifetime in milliseconds | `86400000` |

A sample set of variable names is provided in `.env.example`. Spring Boot does not automatically load `.env` files, so export the variables in the shell, configure them in the IDE, or use an environment-loading tool before starting the application.

## Run Locally

Prerequisites:

- JDK 21.
- Maven 3.9 or newer.
- A running MySQL server for the default application profile.

1. Clone the repository and open the project directory.
2. Create the `expensetrack` MySQL database or use the default JDBC URL's `createDatabaseIfNotExist=true` option.
3. Set database and JWT environment variables as needed.
4. Start the application:

   ```bash
   mvn spring-boot:run
   ```

   Alternatively, package and run the generated jar:

   ```bash
   mvn clean package
   java -jar target/ExpenseTrack-0.0.1-SNAPSHOT.jar
   ```

The application listens on `http://localhost:8080` by default.

## Run Tests

Run the full test suite with:

```bash
mvn test
```

The tests activate the `test` profile and use H2 rather than MySQL. The test suite includes a context-load test and integration coverage for registration, duplicate emails, login/JWT issuance, unauthorized access, expense CRUD, validation, ownership isolation, and analytics.

## Frontend

Open [http://localhost:8080/](http://localhost:8080/) after starting the application. Spring Boot serves `index.html`, `style.css`, and `app.js` from `src/main/resources/static`.

The frontend provides login and registration forms, expense creation and editing, deletion, filters, pagination, overall metrics, category totals, and monthly totals. It stores the JWT in browser `localStorage` under `expenseTrackToken` and uses the same-origin `/api` base path. A `401` response clears the stored token and returns the user to the authentication view.

## OpenAPI Documentation

OpenAPI is configured with a bearer JWT security scheme:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

These routes are permitted without authentication. Use the **Authorize** control in Swagger UI to provide `Bearer <jwt>` for protected operations.

## Project Structure

```text
ExpensC/
├── pom.xml
├── .env.example
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/expensetrack/
│   │   │   ├── ExpenseTrackApplication.java
│   │   │   ├── config/       # Security, JWT properties, OpenAPI
│   │   │   ├── controller/   # Authentication and expense REST controllers
│   │   │   ├── domain/       # User, Expense, and enum types
│   │   │   ├── dto/          # Request, response, and error payloads
│   │   │   ├── exception/    # Exceptions and REST error handling
│   │   │   ├── repository/   # JPA repositories and specifications
│   │   │   ├── security/     # JWT filter/provider and user details
│   │   │   └── service/      # Application business logic
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   │           ├── index.html
│   │           ├── app.js
│   │           └── style.css
│   └── test/
│       ├── java/com/expensetrack/
│       │   ├── ExpenseTrackApplicationTests.java
│       │   └── ExpenseTrackIntegrationTest.java
│       └── resources/application-test.properties
└── target/                   # Generated build output; not source
```

## Security Notes

- Do not commit real database passwords, JWT secrets, or other credentials.
- Replace the development JWT fallback with a long, random secret through `EXPENSETRACK_JWT_SECRET`.
- Keep `.env` files and local secret stores out of Git. The repository's `.gitignore` already excludes `.env`.
- Use a least-privileged MySQL account outside local development and review the Hibernate DDL mode before production use.
