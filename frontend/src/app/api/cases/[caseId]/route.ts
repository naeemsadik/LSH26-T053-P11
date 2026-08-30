import { getCase } from "@/lib/dataset";
import type { CaseData } from "@/lib/types";

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ caseId: string }> },
) {
  const { caseId } = await params;
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
  return Response.json({ case_data: body, saved: true, persistence: "session" });
}
