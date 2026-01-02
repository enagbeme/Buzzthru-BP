# Buzzthru/BP — Employee Time Tracking System

Spring Boot + Thymeleaf time clock and admin dashboard for multi-location businesses.

## Features

- **Employee PIN clock-in/out** (server time only)
- **Multi-location support** (register each store computer to a location)
- **Admin dashboard**
  - Live **Open Shifts** view
  - Employee management (create, reset PIN, activate/deactivate)
  - Location management
- **Real-time admin updates (SSE)**
  - Admin Open Shifts auto-updates on employee clock-in/out
- **Reports**
  - Weekly Hours report (PDF + CSV)
  - Date Range report (PDF + CSV)
- **Admin tools**
  - Time Entries list + **Edit Time Entry** (with required reason)

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Security (admin PIN login)
- Spring Data JPA + Flyway
- Thymeleaf
- OpenPDF (PDF exports)
- Server-Sent Events (SSE)

## Getting Started

### Prerequisites

- Java 17+
- Maven
- MySQL (default) or configure another DB profile

### Configure database

This repo does **not** store DB secrets.
Set env vars before running:

- `DB_USER` (default: `root`)
- `DB_PASSWORD` (default: empty)

The default datasource URL is in `src/main/resources/application.properties`.

### Run the app

```bash
mvn spring-boot:run
```

App runs on:

- `http://localhost:8090`

## Usage

### Time Clock (employees)

- `GET /clock`
- Employees clock in/out using their PIN.

### Admin

- `GET /admin/login`
- Admin dashboard: `GET /admin`

> Note: Admin access requires a SUPER_ADMIN employee PIN.

### Reports

- Weekly report page: `GET /admin/reports/weekly`
  - PDF: `GET /admin/reports/weekly/pdf?weekStart=YYYY-MM-DD`
  - CSV: `GET /admin/reports/weekly/csv?weekStart=YYYY-MM-DD`
- Date range report page: `GET /admin/reports/range`
  - PDF: `GET /admin/reports/range/pdf?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`
  - CSV: `GET /admin/reports/range/csv?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`

### Real-time updates (SSE)

Admin Open Shifts uses:

- Stream: `GET /admin/open-shifts/stream`
- Snapshot: `GET /admin/open-shifts/data`

## Development Notes

- If a page returns `302`, you may need to log in again (Spring Security).
- If you add new endpoints and don’t see them, restart the app.

## License

Proprietary / internal (update as needed).
