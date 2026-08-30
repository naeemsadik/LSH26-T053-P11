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

Without configuration, Next.js route handlers provide a deterministic demo API backed by `../Data/P11_route_shift_public.json`.

Set `NEXT_PUBLIC_API_URL` to an external API root to use the companion backend. Expected paths:

- `POST /plan/generate`
- `POST /plan/validate-move`
- `POST /plan/move`
- `POST /plan/replan-active`
- `GET|POST /cases/:caseId`
- `POST /technicians/:id/sick`

Demo setup edits persist for the current browser session only. Production persistence belongs to the companion backend.
