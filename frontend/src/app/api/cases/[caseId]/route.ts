import { getCase } from "@/lib/dataset";
import {
  backendEnabled,
  backendErrorResponse,
  backendRequest,
  getBackendContext,
  getBackendSnapshot,
  normalizeCase,
  toBackendJob,
  toBackendTechnician,
} from "@/lib/backend";
import type { CaseData } from "@/lib/types";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ caseId: string }> },
) {
  const { caseId } = await params;
  if (backendEnabled()) {
    try {
      return Response.json((await getBackendSnapshot(true)).caseData);
    } catch (error) {
      return backendErrorResponse(error);
    }
  }
  const caseData = getCase(caseId);
  if (!caseData) return Response.json({ error: `Case ${caseId} was not found.` }, { status: 404 });
  return Response.json(caseData);
}

export async function POST(
  request: Request,
  { params }: { params: Promise<{ caseId: string }> },
) {
  const { caseId } = await params;
  const body = await request.json().catch(() => null) as Partial<CaseData> | null;
  if (
    !body ||
    body.case_id !== caseId ||
    !Array.isArray(body.technicians) ||
    !Array.isArray(body.jobs) ||
    !Array.isArray(body.areas) ||
    !body.travel_minutes
  ) {
    return Response.json({ error: "Case setup is incomplete." }, { status: 400 });
  }

  if (backendEnabled()) {
    try {
      const current = await getBackendContext();
      const technicianIds = new Set(current.technicians.map((item) => item.id));
      const jobIds = new Set(current.jobs.map((item) => item.id));
      await Promise.all(body.technicians.map(async (technician) => {
        const payload = toBackendTechnician(technician);
        if (technicianIds.has(technician.id)) {
          const update: Partial<typeof payload> = { ...payload };
          delete update.id;
          await backendRequest(`/technicians/${encodeURIComponent(technician.id)}`, {
            method: "PATCH",
            body: JSON.stringify(update),
          });
        } else {
          await backendRequest("/technicians", { method: "POST", body: JSON.stringify(payload) });
        }
      }));
      await Promise.all(body.jobs.map((job) => backendRequest(
        jobIds.has(job.id) ? `/jobs/${encodeURIComponent(job.id)}` : "/jobs",
        { method: jobIds.has(job.id) ? "PUT" : "POST", body: JSON.stringify(toBackendJob(job)) },
      )));
      return Response.json({ case_data: normalizeCase(await getBackendContext()), saved: true, persistence: "postgres" });
    } catch (error) {
      return backendErrorResponse(error);
    }
  }
  return Response.json({ case_data: body, saved: true, persistence: "session" });
}
