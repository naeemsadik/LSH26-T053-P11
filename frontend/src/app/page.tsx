import Dispatcher from "./dispatcher";
import { connection } from "next/server";
import { backendEnabled, getBackendBaseline, getBackendSnapshot } from "@/lib/backend";
import { getCase, getCaseSummaries } from "@/lib/dataset";
import { generateBaselinePlan, generatePlan } from "@/lib/planner";

export default async function Home() {
  await connection();
  let initialCase = getCase("PUB-01");
  if (!initialCase) throw new Error("Public case PUB-01 is missing.");
  let initialPlan = generatePlan(initialCase);
  let initialBaseline = generateBaselinePlan(initialCase);
  let cases = getCaseSummaries();
  let apiMode: "Live API" | "Browser save" = "Browser save";

  if (backendEnabled()) {
    try {
      const snapshot = await getBackendSnapshot(true);
      initialCase = snapshot.caseData;
      initialPlan = snapshot.plan;
      initialBaseline = await getBackendBaseline();
      cases = [{
        case_id: initialCase.case_id,
        today: initialCase.today,
        technicians: initialCase.technicians.length,
        jobs: initialCase.jobs.length,
      }];
      apiMode = "Live API";
    } catch {
      apiMode = "Browser save";
    }
  }

  return (
    <Dispatcher
      initialCase={initialCase}
      initialPlan={initialPlan}
      initialBaseline={initialBaseline}
      cases={cases}
      apiMode={apiMode}
    />
  );
}
