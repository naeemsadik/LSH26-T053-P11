# Routeboard frontend

Desktop-first dispatcher workspace for P11 route and shift assignment optimisation.

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

Set the server-only `BACKEND_URL` to the Spring API root. Render uses `BACKEND_HOSTPORT` for the private service connection. Without either value, the public-data workspace saves in browser storage for standalone frontend development. Connected paths:

- `POST /plan/generate`
- `POST /plan/validate-move`
- `POST /plan/move`
- `POST /plan/replan-active`
- `GET|POST /cases/:caseId` (Next.js adapter)
- `POST /technicians/:id/sick`

Use `docker compose up --build` from the repository root to run the connected stack.
