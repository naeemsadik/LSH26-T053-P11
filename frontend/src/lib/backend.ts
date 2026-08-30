import type { CaseData, Job, Plan, Technician } from "./types";

type BackendTechnician = {
  id: string;
  name: string;
  skills: string[];
  shiftStart: string;
  shiftEnd: string;
  homeArea: string;
  status: string;
};

type BackendJob = {
  id: string;
  area: string;
  requiredSkill: string;
  durationMinutes: number;
  windowStart: string;
  windowEnd: string;
  status: string;
};

type BackendTravelMatrix = {
  defaultSameAreaBufferMinutes: number;
  travelTimes: Record<string, number>;
};

type BackendPlanResponse = {
  plan: {
    technicianRoutes: Array<{
      technicianId: string;
      orderedStops: Array<{
        jobId: string;
        computedArrival: string;
        computedDeparture: string;
        travelFromPrevious: number;
      }>;
    }>;
  };
  unassigned: Array<{
    jobId: string;
    reasonCode: string;
    reasonText: string;
  }>;
  score: {
    totalTravelMinutes: number;
    jobsScheduledCount: number;
    jobsUnassignedCount: number;
    jobsAtRiskCount: number;
  };
};

export type BackendContext = {
  technicians: BackendTechnician[];
  jobs: BackendJob[];
  matrix: BackendTravelMatrix;
};

export class BackendApiError extends Error {
  constructor(message: string, readonly status = 502) {
    super(message);
  }
}

const backendUrl = () => (process.env.BACKEND_URL ?? "").replace(/\/$/, "");
const trimTime = (value: string) => value.slice(0, 5);
const toEnum = (value: string) => value.trim().toUpperCase().replace(/\s+/g, "_");
const fromEnum = (value: string) => value.toLowerCase().split("_").map((part) => part[0].toUpperCase() + part.slice(1)).join(" ");

function timeToMinutes(value: string): number {
  const [hours, minutes] = trimTime(value).split(":").map(Number);
  return hours * 60 + minutes;
}

export function backendEnabled(): boolean {
  return Boolean(backendUrl());
}

export async function backendRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const root = backendUrl();
  if (!root) throw new BackendApiError("BACKEND_URL is not configured.", 503);

  let response: Response;
  try {
    response = await fetch(`${root}${path}`, {
      ...init,
      cache: "no-store",
      headers: { "Content-Type": "application/json", ...init?.headers },
    });
  } catch {
    throw new BackendApiError("The planning backend is unavailable.");
  }

  const data = await response.json().catch(() => null) as Record<string, unknown> | null;
  if (!response.ok) {
    const details = data?.details as Record<string, unknown> | undefined;
    const message = details?.reason ?? data?.message ?? data?.error ?? `Backend request failed with status ${response.status}.`;
    throw new BackendApiError(String(message), response.status);
  }
  return data as T;
}

export function backendErrorResponse(error: unknown): Response {
  const status = error instanceof BackendApiError ? error.status : 500;
  const message = error instanceof Error ? error.message : "Backend request failed.";
  return Response.json({ error: message }, { status });
}

export async function getBackendContext(): Promise<BackendContext> {
  const [technicians, jobs, matrix] = await Promise.all([
    backendRequest<BackendTechnician[]>("/technicians"),
    backendRequest<BackendJob[]>("/jobs"),
    backendRequest<BackendTravelMatrix>("/travel-matrix"),
  ]);
  return { technicians, jobs, matrix };
}

export function normalizeCase(context: BackendContext): CaseData {
  const areaEnums = [...new Set([
    ...Object.keys(context.matrix.travelTimes).flatMap((key) => key.split("::")),
    ...context.technicians.map((item) => item.homeArea),
    ...context.jobs.map((item) => item.area),
  ])].sort();
  const areas = areaEnums.map(fromEnum);
  const travelMinutes = Object.fromEntries(areaEnums.map((from) => [
    fromEnum(from),
    Object.fromEntries(areaEnums.map((to) => {
      const key = [from, to].sort().join("::");
      const minutes = from === to
        ? context.matrix.travelTimes[key] ?? context.matrix.defaultSameAreaBufferMinutes
        : context.matrix.travelTimes[key];
      return [fromEnum(to), minutes ?? 0];
    })),
  ]));

  return {
    case_id: "LIVE",
    today: new Date().toISOString().slice(0, 10),
    areas,
    travel_minutes: travelMinutes,
    technicians: context.technicians.map((item) => ({
      id: item.id,
      name: item.name,
      skills: item.skills.map((skill) => skill.toLowerCase()),
      shift_start: trimTime(item.shiftStart),
      shift_end: trimTime(item.shiftEnd),
      home_area: fromEnum(item.homeArea),
      status: item.status.toLowerCase() as Technician["status"],
    })),
    jobs: context.jobs.map((item) => ({
      id: item.id,
      area: fromEnum(item.area),
      skill: item.requiredSkill.toLowerCase(),
      duration_minutes: item.durationMinutes,
      window_start: trimTime(item.windowStart),
      window_end: trimTime(item.windowEnd),
      status: item.status === "IN_PROGRESS" ? "in_progress" : item.status === "DONE" ? "done" : "ready",
    })),
  };
}

export function normalizePlan(response: BackendPlanResponse, context: BackendContext): Plan {
  const caseData = normalizeCase(context);
  const jobs = new Map(caseData.jobs.map((job) => [job.id, job]));
  const technicians = new Map(caseData.technicians.map((technician) => [technician.id, technician]));
  const assignments = Object.fromEntries(caseData.technicians.map((technician) => [technician.id, [] as Plan["assignments"][string]]));

  for (const route of response.plan?.technicianRoutes ?? []) {
    const technician = technicians.get(route.technicianId);
    let previousArea = technician?.home_area ?? "";
    assignments[route.technicianId] ??= [];
    for (const stop of route.orderedStops ?? []) {
      const job = jobs.get(stop.jobId);
      if (!job) continue;
      const start = trimTime(stop.computedArrival);
      const margin = timeToMinutes(job.window_end) - timeToMinutes(start);
      assignments[route.technicianId].push({
        job_id: job.id,
        technician_id: route.technicianId,
        area: job.area,
        skill: job.skill,
        duration_minutes: job.duration_minutes,
        window_start: job.window_start,
        window_end: job.window_end,
        start,
        end: trimTime(stop.computedDeparture),
        travel_from: previousArea,
        travel_minutes: stop.travelFromPrevious,
        margin_minutes: margin,
        at_risk: margin <= 10,
      });
      previousArea = job.area;
    }
  }

  const unassigned = (response.unassigned ?? []).flatMap((item) => {
    const job = jobs.get(item.jobId);
    return job ? [{
      job_id: job.id,
      area: job.area,
      skill: job.skill,
      window_start: job.window_start,
      window_end: job.window_end,
      reason_code: item.reasonCode,
      reason_text: item.reasonText,
    }] : [];
  });
  const score = response.score ?? {
    totalTravelMinutes: 0,
    jobsScheduledCount: 0,
    jobsUnassignedCount: unassigned.length,
    jobsAtRiskCount: 0,
  };
  const stats = {
    total_travel_minutes: score.totalTravelMinutes,
    jobs_scheduled: score.jobsScheduledCount,
    jobs_unassigned: score.jobsUnassignedCount,
    jobs_at_risk: score.jobsAtRiskCount,
  };

  return {
    case_id: "LIVE",
    assignments,
    unassigned,
    stats,
    score: Math.max(0, Math.round(100 - stats.total_travel_minutes / 35 - stats.jobs_unassigned * 3.5 - stats.jobs_at_risk * 1.5)),
    generated_at: new Date().toISOString(),
    inactive_technicians: caseData.technicians.filter((item) => item.status === "sick").map((item) => item.id),
  };
}

export async function getBackendSnapshot(ensurePlan = false): Promise<{ caseData: CaseData; plan: Plan }> {
  const context = await getBackendContext();
  let response = await backendRequest<BackendPlanResponse>("/plan/current");
  if (ensurePlan && !(response.plan?.technicianRoutes?.length)) {
    response = await backendRequest<BackendPlanResponse>("/plan/generate", { method: "POST" });
  }
  return { caseData: normalizeCase(context), plan: normalizePlan(response, context) };
}

export function toBackendTechnician(technician: Technician) {
  return {
    id: technician.id,
    name: technician.name,
    skills: technician.skills.map(toEnum),
    shiftStart: technician.shift_start,
    shiftEnd: technician.shift_end,
    homeArea: toEnum(technician.home_area),
    status: toEnum(technician.status ?? "active"),
  };
}

export function toBackendJob(job: Job) {
  return {
    id: job.id,
    area: toEnum(job.area),
    requiredSkill: toEnum(job.skill),
    durationMinutes: job.duration_minutes,
    windowStart: job.window_start,
    windowEnd: job.window_end,
    status: job.status === "in_progress" ? "IN_PROGRESS" : job.status === "done" ? "DONE" : "PENDING",
  };
}

export function movePosition(plan: Plan, jobId: string, technicianId: string, desiredStart: string): number {
  const route = (plan.assignments[technicianId] ?? []).filter((item) => item.job_id !== jobId);
  const index = route.findIndex((item) => item.start > desiredStart);
  return index === -1 ? route.length : index;
}

export type { BackendPlanResponse };
