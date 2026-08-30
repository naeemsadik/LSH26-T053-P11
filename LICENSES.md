# Third-Party Material and AI Disclosure

Material frameworks, libraries, fonts, icons, assets, build tools, and published data used in this repository are listed below.

| Name | Version or source URL | Licence | Used for |
|---|---|---|---|
| Next.js | 16.3.3 / https://nextjs.org | MIT | Frontend application framework, server rendering, API route handlers, and production build |
| React and React DOM | 19.2.8 / https://react.dev | MIT | Frontend component and rendering runtime |
| React Compiler Babel plugin | 1.0.0 / https://github.com/facebook/react | MIT | Build-time React optimization |
| Lucide React | 1.37.0 / https://lucide.dev | ISC | Interface icons |
| Manrope | https://fonts.google.com/specimen/Manrope | SIL Open Font License 1.1 | Body and interface typography through `next/font` |
| Barlow Condensed | https://fonts.google.com/specimen/Barlow+Condensed | SIL Open Font License 1.1 | Display and heading typography through `next/font` |
| Geist Mono | https://fonts.google.com/specimen/Geist+Mono | SIL Open Font License 1.1 | Monospaced operational labels through `next/font` |
| TypeScript | 5.9.3 / https://www.typescriptlang.org | Apache-2.0 | Frontend type checking and compilation |
| ESLint and eslint-config-next | 9.39.5 and 16.3.3 | MIT | Frontend static analysis |
| Playwright Test | 1.62.1 / https://playwright.dev | Apache-2.0 | Browser, responsive-layout, persistence, and API workflow tests |
| Spring Boot | 3.3.3 / https://spring.io/projects/spring-boot | Apache-2.0 | Backend web API, configuration, dependency management, and application runtime |
| Spring Data JPA | Spring Boot 3.3.3 managed / https://spring.io/projects/spring-data-jpa | Apache-2.0 | PostgreSQL persistence repositories |
| Hibernate ORM | Spring Boot 3.3.3 managed / https://hibernate.org/orm | LGPL-2.1-or-later | JPA implementation and entity persistence |
| Spring Validation and Hibernate Validator | Spring Boot 3.3.3 managed / https://hibernate.org/validator | Apache-2.0 | Backend request and model validation |
| springdoc-openapi | 2.5.0 / https://springdoc.org | Apache-2.0 | OpenAPI document and Swagger UI generation |
| PostgreSQL JDBC driver | Spring Boot 3.3.3 managed / https://jdbc.postgresql.org | BSD-2-Clause | Backend PostgreSQL connectivity |
| H2 Database | Spring Boot 3.3.3 managed / https://www.h2database.com | MPL-2.0 or EPL-1.0 | In-memory backend tests |
| PostgreSQL container | 17-alpine / https://hub.docker.com/_/postgres | PostgreSQL Licence | Local Docker Compose database |
| Node.js container | 22-alpine / https://hub.docker.com/_/node | Node.js licence and bundled notices | Frontend Docker build and runtime |
| Eclipse Temurin and Maven containers | Java 21 and Maven 3.9.9 / https://hub.docker.com/_/maven | GPL-2.0 with Classpath Exception and Apache-2.0 | Backend Docker build and Java runtime |

No third-party starter, template, UI kit, stock image, or external illustration asset was used.

## AI tools

| Tool | Used for | How output was verified |
|---|---|---|
| OpenAI Codex | Frontend and backend implementation support, UI refinement, API integration, deployment configuration, tests, debugging, and documentation | Frontend output was checked with ESLint, TypeScript, Playwright browser/API tests across all 25 published cases, responsive screenshots, and the Next.js production build. Backend changes were reviewed against the API contracts and Spring test suite, and the copied seed dataset was parsed and checked for complete area and skill enum coverage. |
| Google Antigravity | Initial Spring Boot backend implementation, assignment and local-search logic, persistence services, backend tests, and supporting documentation | Output was reviewed against the P11 requirements, checked by the Spring unit and acceptance test suite, and compiled successfully during the Maven-based Render Docker build. |

## Original-work statement

Everything not declared in this file or `EVENT.md` was created by the registered team during the event window.
