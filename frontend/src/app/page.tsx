import Dispatcher from "./dispatcher";
import { getCase, getCaseSummaries } from "@/lib/dataset";
import { generatePlan } from "@/lib/planner";

export default function Home() {
  const initialCase = getCase("PUB-01");
  if (!initialCase) throw new Error("Public case PUB-01 is missing.");

  return (
    <Dispatcher
      initialCase={initialCase}
      initialPlan={generatePlan(initialCase)}
      cases={getCaseSummaries()}
    />
  );
}
