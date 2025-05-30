## ss2025-group-e

# Atlas of Climate-related Health Risks

## Project Overview

**Client:** big5health – Association for knowledge transfer and increasing health literacy and joy of life in chronic diseases

This project aims to develop an interactive, map-based information tool that visualizes regionally predicted temperature increases in Austria in relation to common health risks. It also provides behavioral recommendations and contact information for relevant services.

---

## Project Goals

The online atlas offers the following features:

- **Austria Atlas with Zoom and Search**
  - Interactive map with ZIP code search
  - Region-specific visualizations

- **Display of temperature values**
  - Pins with regional temperatures

- **Visualization of mosquito prevalence**
  - Pins with documented mosquito sightings

- **Health Impact Indicators**
  - Selective display of risks (e.g., water quality, pathogen spread)
  - Contextual behavioral recommendations (e.g., fluid intake, sun protection)

- **Contact & Service Points**
  - Heat protection facilities
  - Medical and advisory services

---

## Technology Stack

| Layer         | Technology           |
|---------------|----------------------|
| Frontend      | Angular              |
| Backend       | Java 21 (Spring Boot)|
| Maps          | Leaflet + OpenStreetMap |
| Infrastructure| Gradle, RESTful APIs |
| Data Sources  | ZAMG (weather), AGES (health & pathogens) |

---

## Project Structure

```
project-root/
├── backend/
│   ├── src/
│   │   ├── main/java/at/big5health/klimaatlas/
│   │   │   ├── config/                 # WebClient & caching setup
│   │   │   ├── controllers/            # REST endpoints (Weather, Mosquito)
│   │   │   ├── dtos/                   # Data Transfer Objects (WeatherReportDTO, etc.)
│   │   │   ├── exceptions/             # Custom error handling
│   │   │   ├── grid/                   # Grid & temperature calculation logic
│   │   │   ├── httpclients/            # External API clients
│   │   │   ├── models/                 # Domain models
│   │   │   └── services/               # Business logic services
│   │   └── resources/
│   │       └── application.properties # App configuration
│   ├── test/                          # Unit and integration tests
│   ├── docs/
│   │   └── api-specification.yml      # OpenAPI spec
│   ├── config/                        # Code quality tools (Checkstyle, PMD)
│   ├── build.gradle                   # Gradle build file
│   └── settings.gradle
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── interfaces/              # TypeScript interfaces, types, enums (optionally `models`)
│   │   │   ├── layout/                  # Application layout components (header, footer, nav)
│   │   │   ├── services/                # Services for the apps
│   │   │   ├── utils/                   # Utility functions, constants, formatters
│   │   │   ├── app.component.ts
│   │   │   ├── app.component.html
│   │   │   ├── app.component.scss
│   │   │   ├── app.component.spec.ts
│   │   │   ├── app.config.ts
│   │   │   ├── app.routes.ts
│   │   ├── assets/                     # Static assets (images, icons, etc.)
│   │   ├── index.html
│   │   ├── main.ts
│   │   └── styles.scss
│   ├── e2e/                           # End-to-End Tests
│   ├── test-results/                  # Test result outputs
```

---

## Setup & Installation

### Requirements
- **Java 21**
- **Node.js LTS**
- **Angular CLI**

### Clone project
- git clone https://github.com/fhburgenland-bswe/ss2025-klimaatlas.git
- cd ss2025-klimaatlas

### Install backend
- cd backend
- ./gradlew build

### Start backend
- ./gradlew bootRun

The backend is then accessible at http://localhost:8080.

### Install frontend
- cd frontend
- npm install

### Start frontend
- ng serve

The frontend is then available at http://localhost:4200.

---

## Milestones

| Date         | Event                                      |
|--------------|--------------------------------------------|
| June 2025    | Handover of the online atlas to the client |

---

## License

To be defined by the client (big5health).

---

## Contributors

- Project Team @ big5health
- Software Development Team