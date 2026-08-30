import Dispatcher from "./dispatcher";
import { connection } from "next/server";
import { backendEnabled, getBackendSnapshot } from "@/lib/backend";
import { getCase, getCaseSummaries } from "@/lib/dataset";
import { generatePlan } from "@/lib/planner";

export default async function Home() {
  await connection();
  let initialCase = getCase("PUB-01");
  if (!initialCase) throw new Error("Public case PUB-01 is missing.");
  let initialPlan = generatePlan(initialCase);
  let cases = getCaseSummaries();
  let apiMode: "Live API" | "Demo API" = "Demo API";

  if (backendEnabled()) {
    try {
      const snapshot = await getBackendSnapshot(true);
      initialCase = snapshot.caseData;
      initialPlan = snapshot.plan;
      cases = [{
        case_id: initialCase.case_id,
        today: initialCase.today,
        technicians: initialCase.technicians.length,
        jobs: initialCase.jobs.length,
      }];
      apiMode = "Live API";
    } catch {
      apiMode = "Demo API";
    }
  }

  return (
    <Dispatcher
      initialCase={initialCase}
      initialPlan={initialPlan}
      cases={cases}
      apiMode={apiMode}
    />
  );
}
