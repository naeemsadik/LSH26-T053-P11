import type {
  Assignment,
  CaseData,
  Job,
  MoveValidation,
  Plan,
  PlanStats,
  Technician,
  UnassignedJob,
} from "./types";

export function timeToMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

export function minutesToTime(value: number): string {
  const hours = Math.floor(value / 60).toString().padStart(2, "0");
  const minutes = (value % 60).toString().padStart(2, "0");
  return `${hours}:${minutes}`;
}

function travel(caseData: CaseData, from: string, to: string): number {
  if (from === to) return 0;
  return caseData.travel_minutes[from]?.[to] ?? 0;
}

function assignmentFor(
  job: Job,
  technician: Technician,
  start: number,
  travelFrom: string,
  travelMinutes: number,
): Assignment {
  const margin = timeToMinutes(job.window_end) - start;
  return {
    job_id: job.id,
    technician_id: technician.id,
    area: job.area,
    skill: job.skill,
    duration_minutes: job.duration_minutes,
    window_start: job.window_start,
    window_end: job.window_end,
    start: minutesToTime(start),
    end: minutesToTime(start + job.duration_minutes),
    travel_from: travelFrom,
    travel_minutes: travelMinutes,
    margin_minutes: margin,
    at_risk: margin <= 15,
  };
}

function unassignedReason(caseData: CaseData, job: Job): UnassignedJob {
  const qualified = caseData.technicians.filter(
    (technician) => technician.status !== "sick" && technician.skills.includes(job.skill),
  );

  if (!qualified.length) {
    return {
      job_id: job.id,
      area: job.area,
      skill: job.skill,
      window_start: job.window_start,
      window_end: job.window_end,
      reason_code: "SKILL_UNAVAILABLE",
      reason_text: `No active technician has ${job.skill.replace("_", " ")} certification.`,
    };
  }

  const earliestShift = Math.min(...qualified.map((technician) => timeToMinutes(technician.shift_start)));
  if (timeToMinutes(job.window_end) < earliestShift) {
    return {
      job_id: job.id,
      area: job.area,
      skill: job.skill,
      window_start: job.window_start,
      window_end: job.window_end,
      reason_code: "WINDOW_BEFORE_SHIFT",
      reason_text: `Window closes at ${job.window_end}, before a qualified technician starts.`,
    };
  }

  return {
    job_id: job.id,
    area: job.area,
    skill: job.skill,
    window_start: job.window_start,
    window_end: job.window_end,
    reason_code: "NO_FEASIBLE_ROUTE",
    reason_text: `No route fits the ${job.window_start}-${job.window_end} window, travel, and shift limits.`,
  };
}

function statsFor(assignments: Record<string, Assignment[]>, unassigned: UnassignedJob[]): PlanStats {
  const scheduled = Object.values(assignments).flat();
  return {
    total_travel_minutes: scheduled.reduce((total, item) => total + item.travel_minutes, 0),
    jobs_scheduled: scheduled.length,
    jobs_unassigned: unassigned.length,
    jobs_at_risk: scheduled.filter((item) => item.at_risk).length,
  };
}

function scoreFor(stats: PlanStats): number {
  return Math.max(
    0,
    Math.round(100 - stats.total_travel_minutes / 35 - stats.jobs_unassigned * 3.5 - stats.jobs_at_risk * 1.5),
  );
}

function withFreshStats(plan: Plan): Plan {
  const stats = statsFor(plan.assignments, plan.unassigned);
  return { ...plan, stats, score: scoreFor(stats), generated_at: new Date().toISOString() };
}

export function generatePlan(caseData: CaseData): Plan {
  const assignments = Object.fromEntries(
    caseData.technicians.map((technician) => [technician.id, [] as Assignment[]]),
  );
  const routeState = Object.fromEntries(
    caseData.technicians.map((technician) => [
      technician.id,
      {
        time: timeToMinutes(technician.shift_start),
        area: technician.home_area,
      },
    ]),
  );
  const unassigned: UnassignedJob[] = [];
  const jobs = [...caseData.jobs].sort(
    (a, b) => timeToMinutes(a.window_end) - timeToMinutes(b.window_end) || timeToMinutes(a.window_start) - timeToMinutes(b.window_start),
  );

  for (const job of jobs) {
    const candidates = caseData.technicians
      .filter((technician) => technician.status !== "sick" && technician.skills.includes(job.skill))
      .map((technician) => {
        const state = routeState[technician.id];
        const travelMinutes = travel(caseData, state.area, job.area);
        const arrival = state.time + travelMinutes;
        const start = Math.max(arrival, timeToMinutes(job.window_start));
        return {
          technician,
          start,
          travelMinutes,
          feasible:
            start <= timeToMinutes(job.window_end) &&
            start + job.duration_minutes <= timeToMinutes(technician.shift_end),
        };
      })
      .filter((candidate) => candidate.feasible)
      .sort((a, b) => a.start - b.start || a.travelMinutes - b.travelMinutes);

    const candidate = candidates[0];
    if (!candidate) {
      unassigned.push(unassignedReason(caseData, job));
      continue;
    }

    const state = routeState[candidate.technician.id];
    const assignment = assignmentFor(
      job,
      candidate.technician,
      candidate.start,
      state.area,
      candidate.travelMinutes,
    );
    assignments[candidate.technician.id].push(assignment);
    routeState[candidate.technician.id] = {
      time: timeToMinutes(assignment.end),
      area: job.area,
    };
  }

  const stats = statsFor(assignments, unassigned);
  return {
    case_id: caseData.case_id,
    assignments,
    unassigned,
    stats,
    score: scoreFor(stats),
    generated_at: new Date().toISOString(),
  };
}

function findJob(caseData: CaseData, plan: Plan, jobId: string): Job | undefined {
  const source = Object.values(plan.assignments).flat().find((item) => item.job_id === jobId);
  if (source) {
    return {
      id: source.job_id,
      area: source.area,
      skill: source.skill,
      duration_minutes: source.duration_minutes,
      window_start: source.window_start,
      window_end: source.window_end,
    };
  }
  return caseData.jobs.find((job) => job.id === jobId);
}

export function validateMove(
  caseData: CaseData,
  plan: Plan,
  jobId: string,
  technicianId: string,
  desiredStart: string,
): MoveValidation {
  const job = findJob(caseData, plan, jobId);
  const technician = caseData.technicians.find((item) => item.id === technicianId);

  if (!job || !technician) {
    return { valid: false, reason_code: "NOT_FOUND", reason_text: "Job or technician no longer exists." };
  }
  if (technician.status === "sick" || plan.inactive_technicians?.includes(technicianId)) {
    return { valid: false, reason_code: "TECHNICIAN_UNAVAILABLE", reason_text: `${technician.name} is unavailable for this shift.` };
  }
  if (!technician.skills.includes(job.skill)) {
    return {
      valid: false,
      reason_code: "SKILL_MISMATCH",
      reason_text: `Skill mismatch: ${technician.name} lacks ${job.skill.replace("_", " ")} certification.`,
    };
  }

  const start = timeToMinutes(desiredStart);
  const end = start + job.duration_minutes;
  if (start < timeToMinutes(job.window_start) || start > timeToMinutes(job.window_end)) {
    return {
      valid: false,
      reason_code: "WINDOW_VIOLATION",
      reason_text: `Time window violated: ${job.id} must start between ${job.window_start} and ${job.window_end}.`,
    };
  }
  if (start < timeToMinutes(technician.shift_start) || end > timeToMinutes(technician.shift_end)) {
    return {
      valid: false,
      reason_code: "SHIFT_VIOLATION",
      reason_text: `Shift violated: ${technician.name} works ${technician.shift_start}-${technician.shift_end}.`,
    };
  }

  const route = (plan.assignments[technicianId] ?? [])
    .filter((item) => item.job_id !== jobId)
    .sort((a, b) => timeToMinutes(a.start) - timeToMinutes(b.start));
  const overlap = route.find(
    (item) => start < timeToMinutes(item.end) && end > timeToMinutes(item.start),
  );
  if (overlap) {
    return {
      valid: false,
      reason_code: "JOB_OVERLAP",
      reason_text: `Schedule conflict: ${job.id} overlaps ${overlap.job_id} at ${overlap.start}-${overlap.end}.`,
    };
  }

  const previous = [...route].reverse().find((item) => timeToMinutes(item.end) <= start);
  const next = route.find((item) => timeToMinutes(item.start) >= end);
  const fromArea = previous?.area ?? technician.home_area;
  const availableFrom = previous ? timeToMinutes(previous.end) : timeToMinutes(technician.shift_start);
  const earliestArrival = availableFrom + travel(caseData, fromArea, job.area);

  if (earliestArrival > start) {
    return {
      valid: false,
      reason_code: "TRAVEL_CONFLICT",
      reason_text: `Travel conflict: earliest arrival is ${minutesToTime(earliestArrival)}, after the ${desiredStart} drop time.`,
    };
  }

  if (next) {
    const nextArrival = end + travel(caseData, job.area, next.area);
    if (nextArrival > timeToMinutes(next.start)) {
      return {
        valid: false,
        reason_code: "NEXT_JOB_CONFLICT",
        reason_text: `Travel conflict: ${technician.name} would reach ${next.job_id} at ${minutesToTime(nextArrival)}, after its ${next.start} start.`,
      };
    }
  }

  return { valid: true, normalized_start: minutesToTime(start) };
}

function refreshRouteTravel(caseData: CaseData, technician: Technician, route: Assignment[]): Assignment[] {
  let previousArea = technician.home_area;
  return [...route]
    .sort((a, b) => timeToMinutes(a.start) - timeToMinutes(b.start))
    .map((item) => {
      const travelMinutes = travel(caseData, previousArea, item.area);
      const updated = { ...item, travel_from: previousArea, travel_minutes: travelMinutes };
      previousArea = item.area;
      return updated;
    });
}

export function moveJob(
  caseData: CaseData,
  plan: Plan,
  jobId: string,
  technicianId: string,
  desiredStart: string,
): { validation: MoveValidation; plan?: Plan } {
  const validation = validateMove(caseData, plan, jobId, technicianId, desiredStart);
  if (!validation.valid) return { validation };

  const job = findJob(caseData, plan, jobId)!;
  const technician = caseData.technicians.find((item) => item.id === technicianId)!;
  const assignments = Object.fromEntries(
    Object.entries(plan.assignments).map(([id, route]) => [
      id,
      route.filter((item) => item.job_id !== jobId).map((item) => ({ ...item })),
    ]),
  );
  const targetRoute = assignments[technicianId] ?? [];
  const start = timeToMinutes(validation.normalized_start!);
  const previous = [...targetRoute]
    .filter((item) => timeToMinutes(item.end) <= start)
    .sort((a, b) => timeToMinutes(b.end) - timeToMinutes(a.end))[0];
  const fromArea = previous?.area ?? technician.home_area;

  targetRoute.push(
    assignmentFor(job, technician, start, fromArea, travel(caseData, fromArea, job.area)),
  );
  assignments[technicianId] = targetRoute;

  for (const tech of caseData.technicians) {
    assignments[tech.id] = refreshRouteTravel(caseData, tech, assignments[tech.id] ?? []);
  }

  const nextPlan: Plan = {
    ...plan,
    assignments,
    unassigned: plan.unassigned.filter((item) => item.job_id !== jobId),
  };
  return { validation, plan: withFreshStats(nextPlan) };
}

export function markTechnicianSick(caseData: CaseData, plan: Plan, technicianId: string): Plan {
  const technician = caseData.technicians.find((item) => item.id === technicianId);
  if (!technician) return plan;

  const removed = plan.assignments[technicianId] ?? [];
  const newUnassigned = removed.map<UnassignedJob>((item) => ({
    job_id: item.job_id,
    area: item.area,
    skill: item.skill,
    window_start: item.window_start,
    window_end: item.window_end,
    reason_code: "TECHNICIAN_UNAVAILABLE",
    reason_text: `${technician.name} called in sick; no replacement route has been confirmed.`,
  }));
  const nextPlan: Plan = {
    ...plan,
    assignments: { ...plan.assignments, [technicianId]: [] },
    unassigned: [...plan.unassigned.filter((item) => !removed.some((job) => job.job_id === item.job_id)), ...newUnassigned],
    inactive_technicians: [...new Set([...(plan.inactive_technicians ?? []), technicianId])],
  };
  return withFreshStats(nextPlan);
}
