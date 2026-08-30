# Routeboard

Route and shift assignment workspace with a Next.js frontend, Spring Boot API, and PostgreSQL.

## Local full stack

```bash
docker compose up --build
```

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

Stop with `docker compose down`. Add `-v` only when you intend to delete local database data.

## Render

Create a new Blueprint in Render and select this repository. The root `render.yaml` creates the frontend, API, and PostgreSQL database from the same Dockerfiles used by Compose.

The frontend calls Spring through server-side Next.js route handlers. `BACKEND_URL` therefore remains a runtime server variable and no browser CORS configuration is required.
