export type Technician = {
  id: string;
  name: string;
  skills: string[];
  shift_start: string;
  shift_end: string;
  home_area: string;
  status?: "active" | "sick";
};

export type Job = {
  id: string;
  area: string;
  skill: string;
  duration_minutes: number;
  window_start: string;
  window_end: string;
  status?: "ready" | "in_progress" | "done";
};

export type CaseData = {
  case_id: string;
  today: string;
  areas: string[];
  travel_minutes: Record<string, Record<string, number>>;
  technicians: Technician[];
  jobs: Job[];
  manual_move?: {
    job_id: string;
    to_technician: string;
  };
};

export type Dataset = {
  schema_version: string;
  problem_id: string;
  format_note: string;
  cases: CaseData[];
};

export type CaseSummary = {
  case_id: string;
  today: string;
  technicians: number;
  jobs: number;
};

export type Assignment = {
  job_id: string;
  technician_id: string;
  area: string;
  skill: string;
  duration_minutes: number;
  window_start: string;
  window_end: string;
  start: string;
  end: string;
  travel_from: string;
  travel_minutes: number;
  margin_minutes: number;
  at_risk: boolean;
};

export type UnassignedJob = {
  job_id: string;
  area: string;
  skill: string;
  window_start: string;
  window_end: string;
  reason_code: string;
  reason_text: string;
};

export type PlanStats = {
  total_travel_minutes: number;
  jobs_scheduled: number;
  jobs_unassigned: number;
  jobs_at_risk: number;
};

export type Plan = {
  case_id: string;
  assignments: Record<string, Assignment[]>;
  unassigned: UnassignedJob[];
  stats: PlanStats;
  score: number;
  generated_at: string;
  inactive_technicians?: string[];
};

export type MoveValidation = {
  valid: boolean;
  reason_code?: string;
  reason_text?: string;
  normalized_start?: string;
};
