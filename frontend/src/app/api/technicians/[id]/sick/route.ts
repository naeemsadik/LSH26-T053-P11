import { getCase } from "@/lib/dataset";
import { markTechnicianSick } from "@/lib/planner";
import type { CaseData, Plan } from "@/lib/types";

export async function POST(
  request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const body = await request.json().catch(() => null) as {
    case_id?: string;
    case_data?: CaseData;
    plan?: Plan;
  } | null;
  const caseData = body?.case_data ?? (body?.case_id ? getCase(body.case_id) : undefined);
  if (!caseData || !body?.plan?.assignments) {
    return Response.json({ error: "Case and current plan are required." }, { status: 400 });
  }
  if (!caseData.technicians.some((technician) => technician.id === id)) {
    return Response.json({ error: `Technician ${id} was not found.` }, { status: 404 });
  }
  return Response.json(markTechnicianSick(caseData, body.plan, id));
}
