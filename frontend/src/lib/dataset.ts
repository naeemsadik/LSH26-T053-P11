import "server-only";

import rawDataset from "../../../Data/P11_route_shift_public.json";
import type { CaseData, CaseSummary, Dataset } from "./types";

const dataset = rawDataset as unknown as Dataset;

export function getCase(caseId: string): CaseData | undefined {
  return dataset.cases.find((item) => item.case_id === caseId);
}

export function getCaseSummaries(): CaseSummary[] {
  return dataset.cases.map((item) => ({
    case_id: item.case_id,
    today: item.today,
    technicians: item.technicians.length,
    jobs: item.jobs.length,
  }));
}
