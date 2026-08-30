# Routeboard frontend

Desktop-first dispatcher workspace for P11 route and shift assignment optimisation.

## Live deployment

- Frontend: [https://routeboard-65q1.onrender.com](https://routeboard-65q1.onrender.com/)
- Backend API: [https://routeboard-api.onrender.com](https://routeboard-api.onrender.com)

## Architecture

```mermaid
flowchart LR
    User[Dispatcher] --> UI[React dispatcher workspace]
    UI --> Routes[Next.js API route handlers]
    Routes --> Adapter[backend.ts contract adapter]
    Adapter -->|BACKEND_URL| API[Standalone Spring API]
    UI -. No backend configured .-> Planner[TypeScript planner]
    Planner --> Fixture[P11 public JSON fixture]
    UI -. Browser-mode persistence .-> Storage[(localStorage)]
    UI --> Timeline[Timeline, comparison, and analytics views]
```

## Data flow diagram

```mermaid
flowchart LR
    User[External entity: Dispatcher]
    P1((1.0 Edit dispatch inputs))
    P2((2.0 Request plan operation))
    P3((3.0 Render operational views))
    D1[(D1 Browser workspace)]
    D2[(D2 Public fixture)]
    API[External entity: Spring API]

    User -->|Technician, job, and matrix changes| P1
    P1 -->|Browser-mode save| D1
    P1 -->|Connected-mode save| API
    User -->|Generate, move, emergency, sick day| P2
    D1 -. Browser data .-> P2
    D2 -. Default cases .-> P2
    P2 -->|Connected request| API
    API -->|Plan and validation response| P2
    P2 -->|Assignments, scores, and reasons| P3
    P3 -->|Timeline, alerts, comparison, analytics| User
```

## Run

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

## Verify

```bash
npm run lint
npm test
npm run build
```

Install Playwright Chromium once when needed:

```bash
npx playwright install chromium
```

## Backend

The Spring API lives in the separate [Routeboard backend repository](https://github.com/naeemsadik/LSH26-T053-P11-Backend).

Set the server-only `BACKEND_URL` to its public API root. Render copies `routeboard-api`'s generated `RENDER_EXTERNAL_URL` into this variable. Without `BACKEND_URL` or `BACKEND_HOSTPORT`, the public-data workspace saves in browser storage for standalone frontend development. Connected paths:

- `POST /plan/generate`
- `POST /plan/validate-move`
- `POST /plan/move`
- `POST /plan/replan-active`
- `GET|POST /cases/:caseId` (Next.js adapter)
- `POST /technicians/:id/sick`

Initialize the backend submodule, then use `docker compose up --build` from the repository root to run the connected stack:

```bash
git submodule update --init --recursive
docker compose up --build
```
