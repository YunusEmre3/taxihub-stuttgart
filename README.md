# TaxiHub Stuttgart

A full-stack taxi booking and fleet-dispatch platform for a Stuttgart-based taxi
operator: a public, multi-language booking site for customers, and a
role-separated back office where staff price rides, assign drivers, manage the
fleet and export reports.

Built as a single Spring Boot application (Java 17) that serves both a
server-rendered admin panel and a React single-page booking site, backed by
MySQL, with live geocoding and distance-based fare calculation.

<p align="left">
  <img alt="Java 17"      src="https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white">
  <img alt="Spring Boot"  src="https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?logo=springboot&logoColor=white">
  <img alt="React 19"     src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black">
  <img alt="Vite"         src="https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white">
  <img alt="Tailwind CSS" src="https://img.shields.io/badge/Tailwind-4-06B6D4?logo=tailwindcss&logoColor=white">
  <img alt="MySQL"        src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white">
</p>

---

## Table of contents

- [What it does](#what-it-does)
- [Architecture](#architecture)
- [How to access the sites](#how-to-access-the-sites) — **start here if you are reviewing this project**
- [Running it locally](#running-it-locally)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Security notes](#security-notes)
- [Project structure](#project-structure)
- [Tests](#tests)
- [Known limitations](#known-limitations)

---

## What it does

### Public booking site (no login)

A five-step booking wizard that a customer completes without creating an account.

| Step | What happens |
| --- | --- |
| **Trip** | Pickup and drop-off addresses, date/time, one-way or round trip |
| **Vehicle** | Addresses are geocoded and the route measured; each vehicle class (Standard, Comfort, Van, Business) is priced live from its own tariff |
| **Extras** | Baby/child/booster seat, water, cola, lemonade, orange juice |
| **Contact** | Personal or corporate account, payment method (cash, card, invoice) |
| **Review** | Full price breakdown per leg, then submit |

Wizard progress is persisted to `localStorage`, so a customer who closes the tab
mid-booking returns to the same step. The whole site is available in **Turkish,
German and English**, switchable at any point.

### Staff back office (login required)

| Area | Capability |
| --- | --- |
| **Dashboard** | Operational KPIs, today's bookings, quick status transitions |
| **Bookings** | Create bookings by phone, look one up, track its status |
| **Dispatch** | Assign a driver to a pending booking, ordered by driver proximity |
| **Customers** | Customer directory, built automatically from booking history |
| **Fleet** | Vehicles, plates, class, status (available / maintenance), driver assignment |
| **Reports** | Filterable revenue and volume reporting, with CSV export |
| **Settings** | Per-vehicle-class pricing tariffs, plus each user's own profile |

Accounts are role-separated: `EMPLOYEE` sees the driver-facing dashboard,
`ADMIN` sees the full back office. Login redirects to the right landing page
based on role.

### Cross-cutting

- **Authentication** — registration restricted to the company email domain,
  6-digit email verification, password-reset tokens with a 30-minute lifetime,
  BCrypt password hashing, a custom password policy validator
- **Live routing** — OpenRouteService geocoding and distance, feeding fare calculation
- **Transactional email** — verification codes and reset links via Resend
- **i18n** — TR / DE / EN across both the admin panel and the public site,
  with the choice persisted in a cookie for a year

---

## Architecture

```
                         +------------------------------------------+
   Customer ------------>|  React 19 + Vite + Tailwind              |
   (no account)          |  frontend/  ->  booking wizard           |
                         +----------------+-------------------------+
                                          |  /api/public/**
                                          |  /api/bookings/calculate-route
                         +----------------v-------------------------+
   Staff --------------->|  Spring Boot 3.3.4  (single deployable)  |
   (session login)       |                                          |
                         |  Spring Security  - role-based access    |
                         |  Spring MVC + Thymeleaf - admin panel    |
                         |  Spring Data JPA  - domain + persistence |
                         +---+--------------+-------------------+---+
                             |              |                   |
                        +----v----+   +-----v------+    +-------v------+
                        |  MySQL  |   |   Resend   |    | OpenRoute-   |
                        |    8    |   |  (email)   |    |   Service    |
                        +---------+   +------------+    +--------------+
```

Two deliberate decisions worth calling out:

**One application, two front ends.** The Vite build writes its bundle directly
into `src/main/resources/static/`, so the React booking site and the Thymeleaf
admin panel are served by the same app on the same port. There is no separate
frontend server to deploy, no CORS configuration in the default setup, and one
artifact to ship. `emptyOutDir` is intentionally off so a frontend rebuild does
not wipe the admin panel's own hand-written CSS and JS living in that folder.

**Pricing is data, not code.** Base fare and per-kilometre rate live per vehicle
class in the `pricing_rules` table, seeded once at first start and editable from
the Settings page — changing a tariff is an operator action, not a redeploy.

---

## How to access the sites

This is the part most people need. There are **two separate surfaces**, and they
are reached differently.

### 1. The public booking site — open and use it

| | |
| --- | --- |
| **URL (local)** | <http://localhost:8080/> |
| **Login needed** | No — it is fully anonymous |
| **What to do** | Click *Book now*, enter any two real Stuttgart addresses (e.g. `Stuttgart Hauptbahnhof` to `Flughafen Stuttgart`), and walk through the five steps |
| **Language** | Switch TR / DE / EN with the selector in the top-right corner |

> Address lookup and fare calculation call OpenRouteService, so this step needs
> a valid `ORS_API_KEY` (free — see [Configuration](#configuration)). Without a
> key the wizard still renders, but shows a pricing error at the vehicle step.

### 2. The staff back office — you must create an account first

There is intentionally **no shared demo login committed to this repository**, and
self-registration is locked to the operator's own email domain
(`@stuttgart-taxi.com`) — that restriction is a real requirement of the product,
not an oversight. So a reviewer running this locally creates their own account.
Pick whichever of the two routes below fits your setup.

#### Route A — no email service configured (fastest, recommended for review)

Register with any address on the company domain, then verify and promote the
account directly in the database. This skips the emailed verification code
entirely, so no Resend key is required.

1. Start the app and open <http://localhost:8080/register>
2. Register with an address ending in `@stuttgart-taxi.com` — for example
   `reviewer@stuttgart-taxi.com` — and any password that satisfies the policy
   shown on the form
3. The app will try to email a verification code and fail without a Resend key.
   That is fine — activate the account yourself:

   ```sql
   UPDATE employees
      SET is_verified = 1,
          role        = 'ADMIN'
    WHERE email = 'reviewer@stuttgart-taxi.com';
   ```

4. Log in at <http://localhost:8080/login>. As `ADMIN` you land on the full back
   office. To see the narrower driver view instead, set `role = 'EMPLOYEE'`.

#### Route B — with a Resend API key (exercises the real flow)

With `RESEND_API_KEY` set, registration works end to end: you receive a 6-digit
code by email, confirm it at `/verify-email`, and log in. New accounts are
created as `EMPLOYEE` by design — run the `UPDATE employees SET role = 'ADMIN'`
statement above if you want to see the admin surface too.

#### Want a different email domain?

The allowed domain is a configuration property, not a hard-coded value. Start
the app with your own and register with a normal address:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--company.email-domain=example.com
```

### Route map

Once you are in, here is every entry point and who can reach it.

| Route | Surface | Who can access |
| --- | --- | --- |
| `/` | Public booking wizard | Anyone, no login |
| `/login`, `/register` | Staff sign-in and sign-up | Anyone, no login |
| `/forgot-password`, `/reset-password`, `/verify-email` | Account recovery and activation | Anyone, no login |
| `/dashboard` | Driver / employee dashboard | Any signed-in user |
| `/admin/dashboard` | Admin overview and KPIs | `ADMIN` |
| `/bookings/new`, `/bookings/{id}` | Create and track a booking | `ADMIN` |
| `/drivers` | Driver dispatch board | `ADMIN` |
| `/customers` | Customer directory | `ADMIN` |
| `/vehicles` | Fleet management | `ADMIN` |
| `/reports`, `/reports/export` | Reporting and CSV export | `ADMIN` |
| `/settings` | Pricing tariffs and own profile | `ADMIN` |
| `GET /api/public/pricing`, `GET /api/public/contact` | Read-only public API | Anyone |
| `POST /api/public/bookings` | Public booking submission | Anyone |
| `POST /api/bookings/calculate-route` | Geocode + distance + fare quote | Anyone |

### Seeded demo data

The app ships idempotent seeders that populate vehicles, extra services, pricing
tariffs and a set of sample bookings on first start. Some of them attach to
existing staff accounts, so the richest demo data appears if you **register your
account(s) first, then restart the app once**. Every seeder is guarded by a
row-count check, so restarting never duplicates data.

---

## Running it locally

### Prerequisites

- **JDK 17+**
- **MySQL 8** running on `localhost:3306` (the schema is created automatically)
- **Node.js 20+** (only to build the public booking site)

### Steps

```bash
# 1. Clone
git clone https://github.com/YunusEmre3/taxihub-stuttgart.git
cd taxihub-stuttgart

# 2. Configure - copy the template and fill in your own values
cp .env.example .env

# 3. Build the public booking site into Spring Boot's static folder
cd frontend
npm ci
npm run build
cd ..

# 4. Run
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Then open <http://localhost:8080/>.

The `.env` file is read at startup, is listed in `.gitignore`, and must never be
committed. Every value in it is optional for a first run except the database
credentials — see the table below for what degrades without which key.

### Working on the frontend

```bash
cd frontend
npm run dev      # http://localhost:5173, proxies /api to localhost:8080
npm run lint     # oxlint
```

Run the Spring Boot app alongside it. Vite proxies `/api` to port 8080, so the
same relative paths work in development and in production.

---

## Configuration

All secrets are read from the environment — none are hard-coded, and
`application.properties` only ever references them as `${VAR}`.

| Variable | Required | What breaks without it |
| --- | --- | --- |
| `DB_USERNAME` / `DB_PASSWORD` | **Yes** | The app will not start |
| `ORS_API_KEY` | Recommended | Address lookup and fare calculation fail; the rest of the app works. Free key at [openrouteservice.org](https://openrouteservice.org/dev/#/signup) |
| `RESEND_API_KEY` | Optional | Verification codes and password-reset emails are not sent. Use Route A above to create an account instead. Free key at [resend.com](https://resend.com) |
| `RESEND_FROM_ADDRESS` | Optional | Falls back to a no-reply default |
| `APP_BASE_URL` | Optional | Defaults to `http://localhost:8080`; only affects links inside outgoing emails |
| `CORS_ALLOWED_ORIGINS` | Optional | Only needed when the frontend is hosted separately from the backend — see [Deployment](#deployment) |

Non-secret behaviour lives in `src/main/resources/application.properties`:
allowed registration domain, token lifetimes, the routing provider's base URL,
and the operator's public contact details.

---

## Deployment

### The honest version

This is a stateful Spring Boot application with a MySQL database, so **it does
not run on Vercel** — Vercel's serverless platform has no JVM runtime and no
persistent database.

What *can* go on Vercel is the React booking site, which is a static bundle. So
the deployment splits cleanly in two:

| Piece | Where it goes |
| --- | --- |
| React public booking site (`frontend/`) | **Vercel** — static, global CDN, deploys on every push |
| Spring Boot app + MySQL (admin panel, all APIs) | Any JVM host: Railway, Render, Fly.io, a VPS, or your own machine |

### Deploying the booking site to Vercel

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https://github.com/YunusEmre3/taxihub-stuttgart)

Or import the repository manually:

1. Go to [vercel.com/new](https://vercel.com/new) and import this repository
2. Leave every build setting untouched — [`vercel.json`](vercel.json) in the
   repository root already declares the install command, build command and
   output directory
3. Add one environment variable:

   | Name | Value |
   | --- | --- |
   | `VITE_API_BASE_URL` | The public origin of your running backend, e.g. `https://taxihub-api.example.com` |

4. Deploy

**What works on the Vercel URL without a backend:** the landing page, the whole
wizard shell, language switching and navigation — enough to review the UI.
**What does not:** address lookup, fare calculation and booking submission, since
all three call the Spring Boot API. Point `VITE_API_BASE_URL` at a live backend
and they start working immediately.

### Connecting the two

When the frontend is on a different origin than the backend, the browser
enforces CORS. Set this on the **backend**:

```
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

It is deliberately narrow: it opens only the two endpoints that are already
public (`/api/public/**` and `/api/bookings/calculate-route`). The
session-authenticated admin surface stays same-origin only and is never exposed
cross-origin. Left unset — the default — no CORS headers are sent at all, which
is correct for the normal single-origin deployment.

---

## Security notes

- **No secrets in the repository.** Every credential is an environment variable;
  `.env` is git-ignored and `.env.example` carries placeholders only
- **BCrypt** password hashing via Spring Security's `PasswordEncoder`
- **CSRF protection enabled**, with a documented exemption for the two stateless
  public JSON endpoints, which carry no session and no privileged data
- **Role-based authorization** at the filter chain: the entire back office is
  gated behind `ROLE_ADMIN`, and only an explicit allow-list of paths is public
- **Registration is domain-restricted** to the operator's own email address
  space, with mandatory email verification before an account can log in
- **Password-reset tokens are single-use and time-boxed** (30 minutes);
  verification codes expire after 10
- **Custom password policy** enforced by a Bean Validation constraint

---

## Project structure

```
taxihub-stuttgart/
├── frontend/                       React 19 + Vite public booking site
│   ├── src/components/             Wizard steps, hero, price summary
│   ├── src/state/                  Booking reducer + localStorage persistence
│   ├── src/i18n/                   TR / DE / EN translations and context
│   └── src/api/client.js           Fetch wrappers for the public API
├── src/main/java/com/stuttgarttaxi/taxihub/
│   ├── config/                     Security, MVC/CORS, i18n, data seeders
│   ├── controller/                 MVC controllers + public REST endpoints
│   ├── service/                    Business logic: auth, pricing, routing,
│   │                               booking, reporting, email
│   ├── repository/                 Spring Data JPA repositories
│   ├── entity/                     JPA entities and domain enums
│   ├── dto/                        Form-backing objects and API contracts
│   ├── exception/                  Domain-specific exceptions
│   └── validation/                 Custom password constraint
├── src/main/resources/
│   ├── templates/                  Thymeleaf admin panel
│   ├── static/                     Admin CSS/JS + the built React bundle
│   ├── messages*.properties        TR / DE / EN message bundles
│   └── application.properties      Non-secret configuration
├── vercel.json                     Static deployment config for frontend/
└── .env.example                    Environment variable template
```

---

## Tests

```bash
./mvnw test
```

`RouteServiceIntegrationTest` covers the OpenRouteService integration — the
riskiest part of the system, since fare accuracy depends entirely on it.
Broadening coverage across the service layer is the top item on the list below.

---

## Known limitations

Stated plainly rather than left to be discovered:

- **Test coverage is thin.** One integration test today; the service layer
  deserves unit tests, and the auth flows deserve `spring-security-test` slices
- **Schema is managed by `ddl-auto=update`**, which is convenient in development
  but not safe for production. Flyway migrations are the correct next step
- **Driver locations are seeded, not live.** There is no GPS ingestion yet, so
  proximity-based dispatch ordering runs on placeholder coordinates
- **The public site's error states are minimal.** A failed geocode surfaces a
  message, but there is no retry or offline handling
- **Code comments are in Turkish**, matching the team the project was built for

---

## License

Released under the [MIT License](LICENSE).
