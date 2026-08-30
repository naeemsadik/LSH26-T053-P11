# Route and Shift Assignment Optimiser — Backend v1.0

A high-performance, deterministic Spring Boot 3.x backend engine for technician route optimization, time window assignment, shift constraint enforcement, manual move validation, emergency replanning, and sick technician redistribution.

---

## 1. Overview

Field service operations require matching qualified technicians to customer service jobs while respecting strict time windows, shift boundaries, technician home locations, and geographical travel times. 

This backend acts as the single source of truth for all technician routes and job assignments. It evaluates candidate assignments, calculates exact arrival and departure times, optimizes total travel time across all technicians, and validates manual overrides with complete downstream cascade checking.

---

## 2. Technology & Architecture

- **Java 21**
- **Spring Boot 3.3.3**
- **Maven**
- **Lombok**
- **Springdoc OpenAPI / Swagger 2.5**
- **JUnit 5 & Spring Boot Test**

### Architectural Layers (`com.example.routeoptimizer`)

```
com.example.routeoptimizer
├── config          # OpenApi and Application Configuration
├── controller      # Thin REST Controllers (Technician, Job, TravelMatrix, Plan)
├── dto             # Strongly-typed request/response payloads
├── exception       # Centralized REST Exception Handler (@RestControllerAdvice)
├── model           # Domain models (Technician, Job, Stop, TechnicianRoute, Plan, Score, etc.)
├── seed            # Startup Data Seeder (12+ Techs, 30+ Jobs, Dhaka Travel Matrix)
├── service         # Core business logic and assignment engines
└── validation      # Centralized Hard-Rule & Cascade Validator
```

### In-Memory Application State
As specified in the PRD for v1, persistence is managed using a thread-safe, in-memory service layer (`TechnicianService`, `JobService`, `TravelMatrixService`, `PlanService`) without JPA or PostgreSQL dependencies. The architecture isolates state management so persistence can be swapped in future versions without modifying the optimization core.

---

## 3. Hard Constraints

The backend uses a single, shared `ScheduleValidator` to enforce all hard constraints across automatic planning, manual moves, local search, and sick technician redistribution.

1. **`SKILL_MATCH`**: The assigned technician must possess the required skill for the job (`technician.skills.contains(job.requiredSkill)`).
2. **`SHIFT_BOUNDS`**: The job must fit completely inside the technician's shift boundaries (`computedArrival >= shiftStart` and `computedDeparture <= shiftEnd`).
3. **`TIME_WINDOW`**: Computed arrival must fall within the customer's requested window (`windowStart <= computedArrival <= windowEnd`). Arrival at job $i$ is calculated as:
   $$\text{computedArrival}_i = \max(\text{computedDeparture}_{i-1} + \text{travelTime}(\text{area}_{i-1}, \text{area}_i), \text{windowStart}_i)$$
4. **`CASCADE CHECK`**: Inserting or moving a job into a technician's route recalculates every subsequent stop. If inserting job $J_x$ causes a downstream job $J_y$ to violate its customer window or shift end, the insertion is rejected.

---

## 4. Optimization Engine

The primary objective of the auto-planner (`AssignmentEngine`) is to **minimize total travel time** across all technicians without violating any hard constraints.

### 1. Deterministic Job Sorting
Pending jobs are ordered deterministically by:
1. Narrowest customer window width ($\text{windowEnd} - \text{windowStart}$)
2. Earliest window start time ($\text{windowStart}$)
3. Job ID (tie-breaker)

### 2. Greedy Insertion
For every pending job, the engine evaluates every active technician and every possible insertion position ($0 \le \text{pos} \le N$). For each candidate position:
- The route is simulated using `ScheduleValidator`.
- Infeasible candidate positions are rejected.
- Marginal travel cost is computed:
  $$\Delta \text{Travel} = \text{TotalTravel}_{\text{new}} - \text{TotalTravel}_{\text{old}}$$
- The candidate with the lowest marginal travel cost is selected.
- **Secondary Objective**: If candidates tie on travel cost, the placement minimizing the count of "at-risk" jobs (arrival within 10 minutes of window end) is preferred.

### 3. Local Search Optimization
After greedy insertion, `LocalSearchOptimizer` runs a deterministic local search pass:
- **Pairwise Swaps**: Attempts swapping jobs between technician routes.
- **Relocations**: Attempts moving individual jobs between technician routes.
- Swaps and relocations are accepted **only** if all affected routes remain strictly valid and total travel time strictly decreases.

---

## 5. Unassigned Job Reasoning

When a job cannot be assigned to any technician, `UnassignedReasonService` provides a specific reason code and clear text explanation:

| Reason Code | Condition | Example Reason |
|---|---|---|
| `NO_SKILLED_TECH` | No active technician possesses the required skill. | `"No active technician has the required AC skill."` |
| `WINDOW_MISSED` | Qualified technicians exist, but earliest arrival is past the window end. | `"Nearest qualified technician T03 can arrive at 11:42, but the customer window closes at 11:30."` |
| `SHIFT_CAPACITY` | Qualified technicians exist, but job completion exceeds shift end. | `"All qualified technicians would finish job J05 after their shift ends."` |
| `OTHER` | Any other constraint conflict preventing placement. | `"Job J10 cannot be placed without violating shift bounds or time windows."` |

---

## 6. REST API Endpoints

### Technicians (`/technicians`)
- `GET /technicians`: Retrieve all technicians.
- `POST /technicians`: Create a new technician.
- `PATCH /technicians/{id}`: Update technician fields (`name`, `skills`, `shift_start`, `shift_end`, `home_area`, `status`).
- `POST /technicians/{id}/sick`: Mark technician `SICK`, remove their jobs, redistribute affected jobs into other active routes, return updated plan.

### Jobs (`/jobs`)
- `GET /jobs`: Retrieve all jobs.
- `POST /jobs`: Create a new job (emergency/mid-day job initially `PENDING`).

### Travel Matrix (`/travel-matrix`)
- `GET /travel-matrix`: Retrieve the symmetric travel matrix across Dhaka zones.
- `PUT /travel-matrix`: Update travel time between two zones (validates symmetry and non-negative time).

### Plan Management (`/plan`)
- `POST /plan/generate`: Generate optimized routes for all pending jobs, update job statuses, return plan, unassigned entries, and score.
- `POST /plan/baseline`: Generate unoptimized first-fit baseline schedule for score comparison.
- `GET /plan/current`: Retrieve the current stored plan and score.
- `POST /plan/validate-move`: **Stateless** check of a proposed manual job move (`{ "job_id": "J05", "target_technician_id": "T07", "position": 1 }`). Returns `ValidationResult` without mutating state.
- `POST /plan/move`: Re-validates and commits a manual move, updating route stops and score. Returns `409 CONFLICT` if invalid.
- `POST /plan/replan-active`: Emergency insertion of pending jobs into current plan.

---

## 7. Running the Application

### Prerequisites
- Java 21 JDK
- Maven 3.x

### Command
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
mvn spring-boot:run
```

The application starts on port `8080` and automatically seeds 12 technicians, 30 jobs, and the 10-zone Dhaka travel matrix.

---

## 8. Running Tests

Execute full unit and integration test suite:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
mvn clean test
```

---

## 9. OpenAPI / Swagger Documentation

Access interactive Swagger UI in your browser:
```
http://localhost:8080/swagger-ui.html
```
OpenAPI spec raw JSON:
```
http://localhost:8080/api-docs
```
