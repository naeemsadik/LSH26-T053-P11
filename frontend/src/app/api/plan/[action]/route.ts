import { getCase } from "@/lib/dataset";
import {
  backendEnabled,
  backendErrorResponse,
  backendRequest,
  getBackendContext,
  movePosition,
  normalizePlan,
  toBackendJob,
  type BackendPlanResponse,
} from "@/lib/backend";
import { generatePlan, moveJob, validateMove } from "@/lib/planner";
import type { CaseData, Job, Plan } from "@/lib/types";

function isCaseData(value: unknown): value is CaseData {
  if (!value || typeof value !== "object") return false;
  const item = value as Partial<CaseData>;
  return typeof item.case_id === "string" && Array.isArray(item.technicians) && Array.isArray(item.jobs);
}

function isPlan(value: unknown): value is Plan {
  if (!value || typeof value !== "object") return false;
  const item = value as Partial<Plan>;
  return typeof item.case_id === "string" && !!item.assignments && Array.isArray(item.unassigned);
}

function isJob(value: unknown): value is Job {
  if (!value || typeof value !== "object") return false;
  const item = value as Partial<Job>;
  return (
    typeof item.id === "string" &&
    typeof item.area === "string" &&
    typeof item.skill === "string" &&
    typeof item.duration_minutes === "number" &&
    typeof item.window_start === "string" &&
    typeof item.window_end === "string"
  );
}

export async function POST(
  request: Request,
  { params }: { params: Promise<{ action: string }> },
) {
  const { action } = await params;
  const body = await request.json().catch(() => null);
  if (!body || typeof body !== "object") {
    return Response.json({ error: "Request body must be valid JSON." }, { status: 400 });
  }

  const payload = body as Record<string, unknown>;

  if (backendEnabled()) {
    try {
      if (action === "generate") {
        const response = await backendRequest<BackendPlanResponse>("/plan/generate", { method: "POST" });
        return Response.json(normalizePlan(response, await getBackendContext()));
      }

      if (action === "replan-active") {
        if (!isJob(payload.job)) return Response.json({ error: "Emergency job is incomplete." }, { status: 400 });
        await backendRequest("/jobs", { method: "POST", body: JSON.stringify(toBackendJob(payload.job)) });
        const response = await backendRequest<BackendPlanResponse>("/plan/replan-active", { method: "POST" });
        return Response.json(normalizePlan(response, await getBackendContext()));
      }

      if (!isPlan(payload.plan)) return Response.json({ error: "Current plan is required." }, { status: 400 });
      const jobId = typeof payload.job_id === "string" ? payload.job_id : "";
      const technicianId = typeof payload.to_technician === "string" ? payload.to_technician : "";
      const desiredStart = typeof payload.desired_start === "string" ? payload.desired_start : "";
      if (!jobId || !technicianId || !/^\d{2}:\d{2}$/.test(desiredStart)) {
        return Response.json({ error: "Job, technician, and drop time are required." }, { status: 400 });
      }
      const move = {
        job_id: jobId,
        target_technician_id: technicianId,
        position: movePosition(payload.plan, jobId, technicianId, desiredStart),
      };

      if (action === "validate-move") {
        const result = await backendRequest<{ valid: boolean; broken_rule?: string; reason?: string }>("/plan/validate-move", {
          method: "POST",
          body: JSON.stringify(move),
        });
        return Response.json({ valid: result.valid, reason_code: result.broken_rule, reason_text: result.reason });
      }

      if (action === "move") {
        const response = await backendRequest<BackendPlanResponse>("/plan/move", {
          method: "POST",
          body: JSON.stringify(move),
        });
        return Response.json(normalizePlan(response, await getBackendContext()));
      }

      return Response.json({ error: `Unknown plan action: ${action}.` }, { status: 404 });
    } catch (error) {
      return backendErrorResponse(error);
    }
  }

  const caseId = typeof payload.case_id === "string" ? payload.case_id : "";
  const caseData = isCaseData(payload.case_data) ? payload.case_data : getCase(caseId);
  if (!caseData) {
    return Response.json({ error: `Case ${caseId || "unknown"} was not found.` }, { status: 404 });
  }

  if (action === "generate") {
    await new Promise((resolve) => setTimeout(resolve, 320));
    return Response.json(generatePlan(caseData));
  }

  if (action === "replan-active") {
    if (!isJob(payload.job)) {
      return Response.json({ error: "Emergency job is incomplete." }, { status: 400 });
    }
    return Response.json(generatePlan({ ...caseData, jobs: [...caseData.jobs, payload.job] }));
  }

  if (!isPlan(payload.plan)) {
    return Response.json({ error: "Current plan is required." }, { status: 400 });
  }
  const jobId = typeof payload.job_id === "string" ? payload.job_id : "";
  const technicianId = typeof payload.to_technician === "string" ? payload.to_technician : "";
  const desiredStart = typeof payload.desired_start === "string" ? payload.desired_start : "";
  if (!jobId || !technicianId || !/^\d{2}:\d{2}$/.test(desiredStart)) {
    return Response.json({ error: "Job, technician, and drop time are required." }, { status: 400 });
  }

  if (action === "validate-move") {
    return Response.json(validateMove(caseData, payload.plan, jobId, technicianId, desiredStart));
  }

  if (action === "move") {
    const result = moveJob(caseData, payload.plan, jobId, technicianId, desiredStart);
    if (!result.validation.valid) {
      return Response.json(result.validation, { status: 422 });
    }
    return Response.json(result.plan);
  }

  return Response.json({ error: `Unknown plan action: ${action}.` }, { status: 404 });
}
