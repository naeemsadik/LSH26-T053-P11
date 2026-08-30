# Routeboard

Next.js dispatcher workspace for route and shift assignment optimization.

## Repositories

- Frontend: [naeemsadik/LSH26-T053-P11](https://github.com/naeemsadik/LSH26-T053-P11)
- Backend API: [naeemsadik/LSH26-T053-P11-Backend](https://github.com/naeemsadik/LSH26-T053-P11-Backend)

## Live deployment

- Frontend: [https://routeboard-65q1.onrender.com](https://routeboard-65q1.onrender.com/)
- Backend API: [https://routeboard-api.onrender.com](https://routeboard-api.onrender.com)
- Backend health: [https://routeboard-api.onrender.com/health](https://routeboard-api.onrender.com/health)
- API documentation: [https://routeboard-api.onrender.com/swagger-ui.html](https://routeboard-api.onrender.com/swagger-ui.html)

The `backend` entry in this repository is a Git submodule pointing to the
standalone backend repository. The Spring source and its deployment history
are maintained there, not duplicated here.

## Architecture

```mermaid
flowchart LR
    User[Dispatcher] -->|Plan, setup, analysis| UI[Next.js Routeboard UI]

    subgraph Frontend[Frontend service]
        UI --> Routes[Next.js API route handlers]
        Routes --> Adapter[Backend contract adapter]
        UI -. Browser fallback .-> Planner[TypeScript planner]
        Planner --> Fixture[P11 public JSON fixture]
        UI -. Session persistence .-> BrowserStore[(Browser localStorage)]
    end

    Adapter -->|Server-side HTTP and JSON| API[Spring Boot API]

    subgraph Backend[Standalone backend service]
        API --> Validator[Hard-rule validator]
        API --> Engine[Greedy insertion and local search]
        Engine --> Validator
        API --> Repositories[Spring Data JPA]
        Repositories --> Database[(PostgreSQL)]
        SeedCopy[Published fixture copy] --> Seeder[Empty-database seeder]
        Seeder --> Repositories
    end
```

## Data flow diagram

```mermaid
flowchart LR
    Dispatcher[External entity: Dispatcher]
    P1((1.0 Maintain technicians, jobs, and travel))
    P2((2.0 Generate route plan))
    P3((3.0 Validate manual move))
    P4((4.0 Replan disruption))
    P5((5.0 Present timeline and analytics))
    D1[(D1 PostgreSQL)]
    D2[(D2 Public case fixture)]
    D3[(D3 Browser workspace)]

    Dispatcher -->|Setup changes| P1
    P1 -->|Persisted setup| D1
    P1 -. Standalone save .-> D3
    D1 -->|Skills, shifts, windows, matrix| P2
    D2 -. Fallback case data .-> P2
    D3 -. Saved browser setup .-> P2
    P2 -->|Assignments, score, reasons| P5
    Dispatcher -->|Dragged job and target| P3
    P3 -->|Read current plan and rules| D1
    P3 -->|Valid move or broken rule| P5
    Dispatcher -->|Emergency job or sick technician| P4
    P4 -->|Read and update active plan| D1
    P4 -->|Replanned assignments| P5
    P5 -->|Timeline, alerts, comparisons, analytics| Dispatcher
```

## Local full stack

Clone both repositories and start the containers:

```bash
git clone --recurse-submodules https://github.com/naeemsadik/LSH26-T053-P11.git
cd LSH26-T053-P11
docker compose up --build
```

For an existing clone, initialize the backend once with:

```bash
git submodule update --init --recursive
```

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Render deployment

1. Create a Blueprint from the [backend repository](https://github.com/naeemsadik/LSH26-T053-P11-Backend). It creates `routeboard-api` and `routeboard-db`.
2. Create a Blueprint from this repository. It creates the `routeboard` frontend and injects the existing API's public Render URL.

Do not create the backend as a standalone Web Service. Both Blueprints must be
in the same Render workspace. The frontend calls Spring only through server-side
Next.js route handlers, so no browser CORS configuration is required.

For a manually created frontend service, build the root `Dockerfile` and set
`BACKEND_URL` to the backend's public URL.
