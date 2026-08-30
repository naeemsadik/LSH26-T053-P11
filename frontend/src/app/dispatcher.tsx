"use client";

import {
  AlertTriangle,
  ArrowRight,
  BadgeCheck,
  BarChart3,
  CalendarDays,
  CarFront,
  Check,
  CircleAlert,
  Clock3,
  Download,
  Droplets,
  Flame,
  GitCompareArrows,
  Gauge,
  GripVertical,
  LayoutDashboard,
  LoaderCircle,
  MapPin,
  MapPinned,
  Plus,
  Route,
  Search,
  Siren,
  Snowflake,
  Table2,
  TrendingDown,
  UserRoundX,
  UsersRound,
  Wrench,
  X,
  Zap,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import { CSSProperties, DragEvent, FormEvent, useEffect, useMemo, useState } from "react";
import styles from "./dispatcher.module.css";
import type {
  Assignment,
  CaseData,
  CaseSummary,
  Job,
  MoveValidation,
  Plan,
  Technician,
} from "@/lib/types";

type View = "plan" | "setup" | "analytics" | "compare";
type SetupTab = "technicians" | "jobs" | "matrix";
type TimelineSource = "baseline" | "working";
type SetupModal = "technician" | "job" | null;

type SetupSaveResponse = {
  case_data: CaseData;
  saved: boolean;
  persistence: "postgres" | "browser";
};

type PersistedWorkspace = {
  caseData: CaseData;
  plan: Plan;
  baselinePlan: Plan;
  hasUnsavedChanges: boolean;
  planNeedsRefresh: boolean;
};

const API_ROOT = "/api";
const DAY_START = 8 * 60;
const DAY_END = 21 * 60;
const DAY_SPAN = DAY_END - DAY_START;
const BROWSER_STORAGE_KEY = "routeboard-workspace-v1";

function toMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function toTime(value: number): string {
  return `${Math.floor(value / 60).toString().padStart(2, "0")}:${(value % 60).toString().padStart(2, "0")}`;
}

function formatSkill(value: string): string {
  return value.replace("_", " ").replace(/^./, (letter) => letter.toUpperCase());
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(`${value}T00:00:00Z`));
}

function formatWeekday(value: string): string {
  return new Intl.DateTimeFormat("en-GB", {
    weekday: "long",
    timeZone: "UTC",
  }).format(new Date(`${value}T00:00:00Z`));
}

function blockStyle(start: number, duration: number): CSSProperties {
  return {
    left: `${((start - DAY_START) / DAY_SPAN) * 100}%`,
    width: `${(duration / DAY_SPAN) * 100}%`,
  };
}

function skillIcon(skill: string, size = 13) {
  if (skill === "electrical") return <Zap size={size} />;
  if (skill === "plumbing") return <Droplets size={size} />;
  if (skill === "ac") return <Snowflake size={size} />;
  if (skill === "gas_line") return <Flame size={size} />;
  return <Wrench size={size} />;
}

async function requestJson<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_ROOT}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...options?.headers },
  });
  const data = await response.json().catch(() => ({ error: "Backend returned an unreadable response." }));
  if (!response.ok) throw new Error(data.error ?? data.reason_text ?? `Request failed with status ${response.status}.`);
  return data as T;
}

function detectChangedRoutes(previous: Plan, next: Plan): Set<string> {
  return new Set(
    Object.keys(next.assignments).filter((technicianId) => {
      const before = (previous.assignments[technicianId] ?? []).map((item) => item.job_id).join(",");
      const after = (next.assignments[technicianId] ?? []).map((item) => item.job_id).join(",");
      return before !== after;
    }),
  );
}

function isPersistedWorkspace(value: unknown): value is PersistedWorkspace {
  if (!value || typeof value !== "object") return false;
  const workspace = value as Partial<PersistedWorkspace>;
  return !!workspace.caseData?.case_id && !!workspace.plan?.assignments && !!workspace.baselinePlan?.assignments;
}

function nextRecordId(prefix: string, ids: string[]): string {
  const next = Math.max(0, ...ids.map((id) => Number(id.match(/\d+$/)?.[0] ?? 0))) + 1;
  return `${prefix}${String(next).padStart(2, "0")}`;
}

function Stat({ icon, label, value, note, alert }: { icon: React.ReactNode; label: string; value: string; note: string; alert?: boolean }) {
  return (
    <div className={alert ? `${styles.stat} ${styles.statAlert}` : styles.stat}>
      <span className={styles.statIcon}>{icon}</span>
      <span><small>{label}</small><strong>{value}</strong></span>
      <em>{note}</em>
    </div>
  );
}

function TimelineBoard({
  caseData,
  plan,
  zoom,
  setZoom,
  onSelect,
  onDropJob,
  onSick,
  changedTechnicians,
  editable,
}: {
  caseData: CaseData;
  plan: Plan;
  zoom: number;
  setZoom: (zoom: number) => void;
  onSelect: (assignment: Assignment) => void;
  onDropJob: (jobId: string, technicianId: string, start: string) => void;
  onSick: (technician: Technician) => void;
  changedTechnicians: Set<string>;
  editable: boolean;
}) {
  const hours = Array.from({ length: 14 }, (_, index) => DAY_START / 60 + index);

  const dropOnLane = (event: DragEvent<HTMLDivElement>, technicianId: string) => {
    event.preventDefault();
    if (!editable) return;
    const jobId = event.dataTransfer.getData("text/job-id");
    if (!jobId) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
    const start = Math.round((DAY_START + ratio * DAY_SPAN) / 15) * 15;
    onDropJob(jobId, technicianId, toTime(start));
  };

  return (
    <section className={styles.timelinePanel} aria-label="Technician route timeline">
      <div className={styles.panelHeader}>
        <div>
          <span className={styles.kicker}>Live assignment board</span>
          <h2>Technician timeline</h2>
        </div>
        <div className={styles.timelineTools}>
          <div className={styles.legend} aria-label="Timeline legend">
            <span><i className={styles.legendJob} /> Job</span>
            <span><i className={styles.legendTravel} /> <CarFront size={12} /> Travel</span>
            <span><i className={styles.legendIdle} /> <Clock3 size={12} /> Idle</span>
            <span><i className={styles.legendRisk} /> <AlertTriangle size={12} /> At risk</span>
          </div>
          <div className={styles.zoomControl}>
            <button type="button" title="Zoom out" aria-label="Zoom out" onClick={() => setZoom(Math.max(0.85, zoom - 0.15))}><ZoomOut size={16} /></button>
            <span>{Math.round(zoom * 100)}%</span>
            <button type="button" title="Zoom in" aria-label="Zoom in" onClick={() => setZoom(Math.min(1.6, zoom + 0.15))}><ZoomIn size={16} /></button>
          </div>
        </div>
      </div>

      <div className={styles.timelineScroll}>
        <div className={styles.timelineCanvas} style={{ "--lane-width": `${Math.round(1120 * zoom)}px` } as CSSProperties}>
          <div className={styles.timeRow}>
            <div className={styles.timeCorner}>Technician / shift</div>
            <div className={styles.timeAxis}>
              {hours.map((hour) => <span key={hour} style={{ left: `${((hour * 60 - DAY_START) / DAY_SPAN) * 100}%` }}>{hour.toString().padStart(2, "0")}:00</span>)}
            </div>
          </div>

          {caseData.technicians.map((technician) => {
            const assignments = plan.assignments[technician.id] ?? [];
            const inactive = plan.inactive_technicians?.includes(technician.id);
            let cursor = toMinutes(technician.shift_start);

            return (
              <div className={`${styles.technicianRow} ${changedTechnicians.has(technician.id) ? styles.routeChanged : ""}`} key={technician.id}>
                <div className={styles.technicianCell}>
                  <span className={styles.techAvatar}>{technician.name.slice(0, 1)}</span>
                  <span><strong>{technician.name}</strong><small>{technician.id} / {technician.shift_start}-{technician.shift_end}</small></span>
                  <button type="button" disabled={inactive} title={inactive ? "Technician unavailable" : "Mark technician sick"} aria-label={`Mark ${technician.name} sick`} onClick={() => onSick(technician)}><UserRoundX size={15} /></button>
                </div>
                <div className={inactive ? `${styles.lane} ${styles.laneInactive}` : styles.lane} onDragOver={(event) => editable && event.preventDefault()} onDrop={(event) => dropOnLane(event, technician.id)}>
                  <span className={styles.offShift} style={blockStyle(DAY_START, Math.max(0, toMinutes(technician.shift_start) - DAY_START))} />
                  <span className={styles.offShift} style={blockStyle(toMinutes(technician.shift_end), Math.max(0, DAY_END - toMinutes(technician.shift_end)))} />
                  {assignments.map((assignment) => {
                    const start = toMinutes(assignment.start);
                    const travelStart = start - assignment.travel_minutes;
                    const idleDuration = Math.max(0, travelStart - cursor);
                    const idleStart = cursor;
                    cursor = toMinutes(assignment.end);
                    return (
                      <span key={assignment.job_id}>
                        {idleDuration >= 15 && <span className={styles.idleBlock} style={blockStyle(idleStart, idleDuration)} title={`${idleDuration} minutes idle`}><Clock3 size={11} /><b>{idleDuration}m</b></span>}
                        {assignment.travel_minutes > 0 && <span className={styles.travelBlock} style={blockStyle(travelStart, assignment.travel_minutes)} title={`${assignment.travel_minutes} minutes travel from ${assignment.travel_from}`}><CarFront size={11} /><b>{assignment.travel_minutes}m</b></span>}
                        <button
                          type="button"
                          draggable={editable}
                          className={`${styles.jobBlock} ${styles[`skill_${assignment.skill}`]} ${assignment.at_risk ? styles.atRisk : ""}`}
                          style={blockStyle(start, assignment.duration_minutes)}
                          onDragStart={(event) => {
                            event.dataTransfer.setData("text/job-id", assignment.job_id);
                            event.dataTransfer.effectAllowed = "move";
                          }}
                          onClick={() => onSelect(assignment)}
                          title={`${assignment.job_id}: ${assignment.area}, ${assignment.start}-${assignment.end}`}
                        >
                          <GripVertical className={styles.dragGrip} size={12} />
                          <span>{skillIcon(assignment.skill)}<strong>{assignment.job_id}</strong></span>
                          <small>{assignment.area}</small>
                          {assignment.at_risk && <AlertTriangle className={styles.riskIcon} size={12} />}
                        </button>
                      </span>
                    );
                  })}
                  {!inactive && cursor < toMinutes(technician.shift_end) && toMinutes(technician.shift_end) - cursor >= 15 && (
                    <span className={styles.idleBlock} style={blockStyle(cursor, toMinutes(technician.shift_end) - cursor)}><Clock3 size={11} /><b>Idle</b></span>
                  )}
                  {inactive && <span className={styles.unavailableLabel}>Unavailable</span>}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}

export default function Dispatcher({
  initialCase,
  initialPlan,
  initialBaseline,
  cases,
  apiMode,
}: {
  initialCase: CaseData;
  initialPlan: Plan;
  initialBaseline: Plan;
  cases: CaseSummary[];
  apiMode: "Live API" | "Browser save";
}) {
  const [activeView, setActiveView] = useState<View>("plan");
  const [setupTab, setSetupTab] = useState<SetupTab>("technicians");
  const [caseData, setCaseData] = useState(initialCase);
  const [plan, setPlan] = useState(initialPlan);
  const [baselinePlan, setBaselinePlan] = useState(initialBaseline);
  const [timelineSource, setTimelineSource] = useState<TimelineSource>("working");
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);
  const [zoom, setZoom] = useState(1);
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [validationError, setValidationError] = useState("");
  const [changedTechnicians, setChangedTechnicians] = useState<Set<string>>(new Set());
  const [showEmergency, setShowEmergency] = useState(false);
  const [setupModal, setSetupModal] = useState<SetupModal>(null);
  const [jobSearch, setJobSearch] = useState("");
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [planNeedsRefresh, setPlanNeedsRefresh] = useState(false);
  const [browserStorageReady, setBrowserStorageReady] = useState(false);
  const [matrixFrom, setMatrixFrom] = useState(initialCase.areas[0]);
  const [matrixTo, setMatrixTo] = useState(initialCase.areas[1] ?? initialCase.areas[0]);

  useEffect(() => {
    const restore = window.setTimeout(() => {
      const [view, tab] = window.location.hash.slice(1).split("/");
      if (view === "plan" || view === "setup" || view === "analytics" || view === "compare") setActiveView(view);
      if (tab === "technicians" || tab === "jobs" || tab === "matrix") setSetupTab(tab);

      if (apiMode === "Browser save") {
        try {
          const saved = JSON.parse(window.localStorage.getItem(BROWSER_STORAGE_KEY) ?? "null");
          if (isPersistedWorkspace(saved)) {
            setCaseData(saved.caseData);
            setPlan(saved.plan);
            setBaselinePlan(saved.baselinePlan);
            setHasUnsavedChanges(saved.hasUnsavedChanges);
            setPlanNeedsRefresh(saved.planNeedsRefresh);
            setMatrixFrom(saved.caseData.areas[0]);
            setMatrixTo(saved.caseData.areas[1] ?? saved.caseData.areas[0]);
          }
        } catch {
          window.localStorage.removeItem(BROWSER_STORAGE_KEY);
        }
      }
      setBrowserStorageReady(true);
      document.documentElement.dataset.routeboardReady = "true";
    }, 0);
    return () => {
      window.clearTimeout(restore);
      delete document.documentElement.dataset.routeboardReady;
    };
  }, [apiMode]);

  useEffect(() => {
    if (apiMode !== "Browser save" || !browserStorageReady) return;
    const workspace: PersistedWorkspace = { caseData, plan, baselinePlan, hasUnsavedChanges, planNeedsRefresh };
    window.localStorage.setItem(BROWSER_STORAGE_KEY, JSON.stringify(workspace));
  }, [apiMode, baselinePlan, browserStorageReady, caseData, hasUnsavedChanges, plan, planNeedsRefresh]);

  const displayPlan = timelineSource === "baseline" ? baselinePlan : plan;
  const filteredJobs = useMemo(() => {
    const query = jobSearch.trim().toLowerCase();
    return query
      ? caseData.jobs.filter((job) => `${job.id} ${job.area} ${formatSkill(job.skill)}`.toLowerCase().includes(query))
      : caseData.jobs;
  }, [caseData.jobs, jobSearch]);
  const analytics = useMemo(() => {
    const scheduled = Object.values(plan.assignments).flat();
    const scheduledIds = new Set(scheduled.map((item) => item.job_id));
    const activeTechnicians = caseData.technicians.filter((item) => item.status !== "sick");
    const availableMinutes = activeTechnicians.reduce(
      (total, item) => total + Math.max(0, toMinutes(item.shift_end) - toMinutes(item.shift_start)),
      0,
    );
    const serviceMinutes = scheduled.reduce((total, item) => total + item.duration_minutes, 0);
    const usedMinutes = serviceMinutes + plan.stats.total_travel_minutes;
    const areaRows = caseData.areas.map((area) => {
      const jobs = caseData.jobs.filter((item) => item.area === area);
      return {
        area,
        total: jobs.length,
        scheduled: jobs.filter((item) => scheduledIds.has(item.id)).length,
      };
    }).sort((a, b) => b.total - a.total || a.area.localeCompare(b.area));
    const maxAreaJobs = Math.max(1, ...areaRows.map((item) => item.total));
    const skillRows = [...new Set(caseData.jobs.map((item) => item.skill))].map((skill) => ({
      skill,
      jobs: caseData.jobs.filter((item) => item.skill === skill).length,
      technicians: activeTechnicians.filter((item) => item.skills.includes(skill)).length,
    })).sort((a, b) => b.jobs - a.jobs);
    const workloads = caseData.technicians.map((technician) => {
      const route = plan.assignments[technician.id] ?? [];
      const shiftMinutes = Math.max(1, toMinutes(technician.shift_end) - toMinutes(technician.shift_start));
      const service = route.reduce((total, item) => total + item.duration_minutes, 0);
      const travel = route.reduce((total, item) => total + item.travel_minutes, 0);
      return {
        technician,
        jobs: route.length,
        travel,
        utilization: technician.status === "sick" ? 0 : Math.min(100, Math.round(((service + travel) / shiftMinutes) * 100)),
      };
    }).sort((a, b) => b.utilization - a.utilization || a.technician.name.localeCompare(b.technician.name));

    return {
      coverage: caseData.jobs.length ? Math.round((plan.stats.jobs_scheduled / caseData.jobs.length) * 100) : 0,
      utilization: availableMinutes ? Math.round((usedMinutes / availableMinutes) * 100) : 0,
      travelPerJob: plan.stats.jobs_scheduled ? Math.round(plan.stats.total_travel_minutes / plan.stats.jobs_scheduled) : 0,
      travelSaved: baselinePlan.stats.total_travel_minutes - plan.stats.total_travel_minutes,
      areaRows,
      maxAreaJobs,
      skillRows,
      workloads,
    };
  }, [baselinePlan.stats.total_travel_minutes, caseData, plan]);

  const notify = (message: string) => {
    setNotice(message);
    window.setTimeout(() => setNotice(""), 2800);
  };

  const markSetupChanged = () => {
    setHasUnsavedChanges(true);
    setPlanNeedsRefresh(true);
  };

  const openView = (view: View, tab = setupTab) => {
    setActiveView(view);
    window.history.replaceState(null, "", view === "setup" ? `#setup/${tab}` : `#${view}`);
  };

  const openSetupTab = (tab: SetupTab) => {
    setSetupTab(tab);
    setActiveView("setup");
    window.history.replaceState(null, "", `#setup/${tab}`);
  };

  const generate = async (nextCase = caseData) => {
    setBusyAction("generate");
    setError("");
    setValidationError("");
    try {
      const nextPlan = await requestJson<Plan>("/plan/generate", {
        method: "POST",
        body: JSON.stringify({ case_id: nextCase.case_id, case_data: nextCase }),
      });
      setPlan(nextPlan);
      setTimelineSource("working");
      setHasUnsavedChanges(false);
      setPlanNeedsRefresh(false);
      const nextBaseline = await requestJson<Plan>("/plan/baseline", {
        method: "POST",
        body: JSON.stringify({ case_id: nextCase.case_id, case_data: nextCase }),
      });
      setBaselinePlan(nextBaseline);
      notify(`Plan generated: ${nextPlan.stats.jobs_scheduled} jobs scheduled.`);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Plan generation failed.");
    } finally {
      setBusyAction("");
    }
  };

  const selectCase = async (caseId: string) => {
    setBusyAction("case");
    setError("");
    try {
      const nextCase = await requestJson<CaseData>(`/cases/${caseId}`);
      const nextPlan = await requestJson<Plan>("/plan/generate", {
        method: "POST",
        body: JSON.stringify({ case_id: nextCase.case_id, case_data: nextCase }),
      });
      const nextBaseline = await requestJson<Plan>("/plan/baseline", {
        method: "POST",
        body: JSON.stringify({ case_id: nextCase.case_id, case_data: nextCase }),
      });
      setCaseData(nextCase);
      setMatrixFrom(nextCase.areas[0]);
      setMatrixTo(nextCase.areas[1] ?? nextCase.areas[0]);
      setJobSearch("");
      setPlan(nextPlan);
      setBaselinePlan(nextBaseline);
      setTimelineSource("working");
      setSelectedAssignment(null);
      setHasUnsavedChanges(false);
      setPlanNeedsRefresh(false);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Case loading failed.");
    } finally {
      setBusyAction("");
    }
  };

  const dropJob = async (jobId: string, technicianId: string, desiredStart: string) => {
    setBusyAction(jobId);
    setValidationError("");
    setError("");
    const payload = {
      case_id: caseData.case_id,
      case_data: caseData,
      plan,
      job_id: jobId,
      to_technician: technicianId,
      desired_start: desiredStart,
    };
    try {
      const validation = await requestJson<MoveValidation>("/plan/validate-move", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      if (!validation.valid) {
        setValidationError(validation.reason_text ?? "Move violates a planning rule.");
        return;
      }
      const movedPlan = await requestJson<Plan>("/plan/move", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setChangedTechnicians(detectChangedRoutes(plan, movedPlan));
      setPlan(movedPlan);
      setTimelineSource("working");
      setSelectedAssignment(null);
      notify(`${jobId} moved and route times refreshed.`);
      window.setTimeout(() => setChangedTechnicians(new Set()), 1800);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Move could not be completed.");
    } finally {
      setBusyAction("");
    }
  };

  const markSick = async (technician: Technician) => {
    if (!window.confirm(`Mark ${technician.name} unavailable and release all pending jobs?`)) return;
    setBusyAction(technician.id);
    setError("");
    try {
      const nextPlan = await requestJson<Plan>(`/technicians/${technician.id}/sick`, {
        method: "POST",
        body: JSON.stringify({ case_id: caseData.case_id, case_data: caseData, plan }),
      });
      const nextCase = {
        ...caseData,
        technicians: caseData.technicians.map((item) => item.id === technician.id ? { ...item, status: "sick" as const } : item),
      };
      setCaseData(nextCase);
      setPlan(nextPlan);
      setTimelineSource("working");
      setChangedTechnicians(new Set([...detectChangedRoutes(plan, nextPlan), technician.id]));
      const nextBaseline = await requestJson<Plan>("/plan/baseline", {
        method: "POST",
        body: JSON.stringify({ case_id: nextCase.case_id, case_data: nextCase }),
      });
      setBaselinePlan(nextBaseline);
      notify(`${technician.name} marked unavailable; remaining work redistributed.`);
      window.setTimeout(() => setChangedTechnicians(new Set()), 1800);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Technician status could not be changed.");
    } finally {
      setBusyAction("");
    }
  };

  const addEmergency = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const job: Job = {
      id: `E${String(caseData.jobs.filter((item) => item.id.startsWith("E")).length + 1).padStart(2, "0")}`,
      area: String(values.get("area")),
      skill: String(values.get("skill")),
      duration_minutes: Number(values.get("duration")),
      window_start: String(values.get("window_start")),
      window_end: String(values.get("window_end")),
    };
    setBusyAction("emergency");
    setError("");
    try {
      const nextCase = { ...caseData, jobs: [...caseData.jobs, job] };
      const nextPlan = await requestJson<Plan>("/plan/replan-active", {
        method: "POST",
        body: JSON.stringify({ case_id: caseData.case_id, case_data: caseData, job }),
      });
      setChangedTechnicians(detectChangedRoutes(plan, nextPlan));
      setCaseData(nextCase);
      setPlan(nextPlan);
      setTimelineSource("working");
      setShowEmergency(false);
      const nextBaseline = await requestJson<Plan>("/plan/baseline", {
        method: "POST",
        body: JSON.stringify({ case_id: nextCase.case_id, case_data: nextCase }),
      });
      setBaselinePlan(nextBaseline);
      notify(`${job.id} added; active routes replanned.`);
      window.setTimeout(() => setChangedTechnicians(new Set()), 1800);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Emergency job could not be added.");
    } finally {
      setBusyAction("");
    }
  };

  const saveSetup = async () => {
    setBusyAction("save");
    setError("");
    try {
      const saved = await requestJson<SetupSaveResponse>(`/cases/${caseData.case_id}`, {
        method: "POST",
        body: JSON.stringify(caseData),
      });
      const nextPlan = await requestJson<Plan>("/plan/generate", {
        method: "POST",
        body: JSON.stringify({ case_id: saved.case_data.case_id, case_data: saved.case_data }),
      });
      const nextBaseline = await requestJson<Plan>("/plan/baseline", {
        method: "POST",
        body: JSON.stringify({ case_id: saved.case_data.case_id, case_data: saved.case_data }),
      });
      setCaseData(saved.case_data);
      setPlan(nextPlan);
      setBaselinePlan(nextBaseline);
      setTimelineSource("working");
      setHasUnsavedChanges(false);
      setPlanNeedsRefresh(false);
      notify(apiMode === "Live API" ? "Setup and plan saved." : "Setup and plan saved in this browser.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Setup could not be saved.");
    } finally {
      setBusyAction("");
    }
  };

  const updateTechnician = (index: number, field: keyof Technician, value: string | string[]) => {
    markSetupChanged();
    setCaseData((current) => ({
      ...current,
      technicians: current.technicians.map((technician, row) => row === index ? { ...technician, [field]: value } : technician),
    }));
  };

  const updateJob = (index: number, field: keyof Job, value: string | number) => {
    markSetupChanged();
    const jobId = filteredJobs[index]?.id;
    setCaseData((current) => ({
      ...current,
      jobs: current.jobs.map((job) => job.id === jobId ? { ...job, [field]: value } : job),
    }));
  };

  const addTechnician = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const name = String(values.get("name")).trim();
    const skills = values.getAll("skills").map(String);
    const shiftStart = String(values.get("shift_start"));
    const shiftEnd = String(values.get("shift_end"));
    if (!name || !skills.length || shiftStart >= shiftEnd) {
      setError(!name ? "Enter a technician name." : !skills.length ? "Select at least one skill." : "Shift end must be after shift start.");
      return;
    }

    const technician: Technician = {
      id: nextRecordId("T", caseData.technicians.map((item) => item.id)),
      name,
      skills,
      shift_start: shiftStart,
      shift_end: shiftEnd,
      home_area: String(values.get("home_area")),
      status: "active",
    };
    markSetupChanged();
    setCaseData((current) => ({ ...current, technicians: [...current.technicians, technician] }));
    setSetupModal(null);
    notify(`${technician.name} added. Save setup to keep it.`);
  };

  const addJob = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const windowStart = String(values.get("window_start"));
    const windowEnd = String(values.get("window_end"));
    if (windowStart >= windowEnd) {
      setError("Window end must be after window start.");
      return;
    }

    const job: Job = {
      id: nextRecordId("J", caseData.jobs.map((item) => item.id)),
      area: String(values.get("area")),
      skill: String(values.get("skill")),
      duration_minutes: Number(values.get("duration")),
      window_start: windowStart,
      window_end: windowEnd,
      status: "ready",
    };
    markSetupChanged();
    setCaseData((current) => ({ ...current, jobs: [...current.jobs, job] }));
    setSetupModal(null);
    notify(`${job.id} added. Save setup to keep it.`);
  };

  const updateTravelTime = (from: string, to: string, minutes: number) => {
    if (!Number.isFinite(minutes) || minutes < 0) return;
    markSetupChanged();
    setCaseData((current) => ({
      ...current,
      travel_minutes: {
        ...current.travel_minutes,
        [from]: { ...current.travel_minutes[from], [to]: minutes },
        [to]: { ...current.travel_minutes[to], [from]: minutes },
      },
    }));
  };

  const exportPlan = () => {
    const url = URL.createObjectURL(new Blob([JSON.stringify(displayPlan, null, 2)], { type: "application/json" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `${caseData.case_id.toLowerCase()}-plan.json`;
    link.click();
    URL.revokeObjectURL(url);
    notify("Plan exported.");
  };

  const views = [
    { id: "plan" as const, label: "Plan", icon: LayoutDashboard },
    { id: "analytics" as const, label: "Analytics", icon: BarChart3 },
    { id: "compare" as const, label: "Compare", icon: GitCompareArrows },
  ];
  const dataViews = [
    { id: "technicians" as const, label: "Technicians", icon: UsersRound, count: caseData.technicians.length },
    { id: "jobs" as const, label: "Jobs", icon: Wrench, count: caseData.jobs.length },
    { id: "matrix" as const, label: "Travel times", icon: Table2, count: caseData.areas.length },
  ];
  const setupCopy = {
    technicians: { kicker: "Team setup", title: "Technicians", description: `Skills and shift availability for ${formatDate(caseData.today)}.` },
    jobs: { kicker: "Work setup", title: "Jobs", description: `${caseData.jobs.length} service requests for this dispatch date.` },
    matrix: { kicker: "Travel setup", title: "Travel times", description: "Edit the drive time between any two service areas." },
  }[setupTab];
  const statsPlan = activeView === "analytics" ? plan : displayPlan;
  const stats = statsPlan.stats;

  return (
    <main className={styles.app}>
      <aside className={styles.sidebar}>
        <button className={styles.brand} type="button" onClick={() => openView("plan")} aria-label="Routeboard home">
          <span><Route size={23} /></span>
          <strong>Routeboard<small>Dispatch optimiser</small></strong>
        </button>

        <span className={styles.sidebarLabel}>Workspace</span>
        <nav className={styles.mainNav} aria-label="Workspace views">
          {views.map((view) => { const Icon = view.icon; return <button type="button" key={view.id} className={activeView === view.id ? styles.navActive : ""} onClick={() => openView(view.id)}><Icon size={16} />{view.label}</button>; })}
        </nav>

        <span className={styles.sidebarLabel}>Data setup</span>
        <nav className={styles.dataNav} aria-label="Data setup views">
          {dataViews.map((view) => { const Icon = view.icon; return <button type="button" key={view.id} className={activeView === "setup" && setupTab === view.id ? styles.navActive : ""} onClick={() => openSetupTab(view.id)}><Icon size={16} /><span>{view.label}</span><small>{view.count}</small></button>; })}
        </nav>

        <span className={styles.sidebarLabel}>Day controls</span>
        <div className={styles.sidebarActions}>
          <button className={styles.generateButton} type="button" onClick={() => generate()} disabled={!!busyAction}>{busyAction === "generate" ? <LoaderCircle className={styles.spinner} size={17} /> : <Route size={17} />} Generate plan</button>
          <button className={styles.emergencyButton} type="button" onClick={() => { if (hasUnsavedChanges) { openSetupTab(setupTab); setError("Save setup before adding an emergency job."); } else { setShowEmergency(true); } }}><Siren size={17} /> Emergency job</button>
        </div>
        <div className={styles.sidebarFooter}>
          <span className={styles.apiStatus}><i />{apiMode}</span>
        </div>
      </aside>

      <header className={styles.topbar}>
        <div className={styles.dateContext}>
          <span className={styles.dateIcon}><CalendarDays size={19} /></span>
          <span className={styles.datePrimary}><small>Operating date</small><strong>{formatDate(caseData.today)}</strong></span>
          <span className={styles.dateSummary}><b>{formatWeekday(caseData.today)}</b><small>{caseData.technicians.length} technicians / {caseData.jobs.length} jobs / {caseData.areas.length} areas</small></span>
        </div>
        <label className={styles.caseSelect}><span>Case</span><select value={caseData.case_id} onChange={(event) => selectCase(event.target.value)} disabled={!!busyAction}>{cases.map((item) => <option key={item.case_id} value={item.case_id}>{item.case_id} / {item.technicians} tech / {item.jobs} jobs</option>)}</select></label>
      </header>

      <div className={styles.workspace}>
      {(activeView === "plan" || activeView === "analytics" || activeView === "compare") && (
        <section className={styles.statsBar} aria-label="Current plan statistics">
          <div className={styles.scoreStat}><span>Plan score</span><strong>{statsPlan.score}</strong><em>/100</em></div>
          <Stat icon={<CarFront size={18} />} label="Total travel" value={`${Math.floor(stats.total_travel_minutes / 60)}h ${stats.total_travel_minutes % 60}m`} note="Across all routes" />
          <Stat icon={<BadgeCheck size={18} />} label="Jobs scheduled" value={String(stats.jobs_scheduled)} note={`${caseData.jobs.length} jobs received`} />
          <Stat icon={<CircleAlert size={18} />} label="Unassigned" value={String(stats.jobs_unassigned)} note={stats.jobs_unassigned ? "Needs dispatcher action" : "All work covered"} alert={stats.jobs_unassigned > 0} />
          <Stat icon={<AlertTriangle size={18} />} label="At risk" value={String(stats.jobs_at_risk)} note="10 min margin or less" alert={stats.jobs_at_risk > 0} />
          <Stat icon={<UsersRound size={18} />} label="Technicians idle" value={String(caseData.technicians.filter((item) => item.status !== "sick" && !(displayPlan.assignments[item.id]?.length)).length)} note="No assigned jobs" />
        </section>
      )}

      {(error || validationError || notice) && <div className={`${styles.banner} ${error || validationError ? styles.bannerError : styles.bannerSuccess}`} role="status">{error || validationError ? <CircleAlert size={16} /> : <Check size={16} />}<span>{error || validationError || notice}</span>{(error || validationError) && <button type="button" aria-label="Dismiss message" title="Dismiss" onClick={() => { setError(""); setValidationError(""); }}><X size={16} /></button>}</div>}

      {activeView === "plan" && (
        <section className={styles.planWorkspace}>
          <div className={styles.planMain}>
            <div className={styles.planToolbar}>
              <div><span className={styles.kicker}>{formatWeekday(caseData.today)} dispatch</span><h1>{caseData.case_id} day plan</h1><p>{caseData.technicians.length} technicians across {caseData.areas.length} Dhaka service areas</p></div>
              <div className={styles.toolbarRight}>
                <div className={styles.sourceToggle} aria-label="Timeline plan source"><button type="button" className={timelineSource === "baseline" ? styles.sourceActive : ""} onClick={() => setTimelineSource("baseline")}>Baseline</button><button type="button" className={timelineSource === "working" ? styles.sourceActive : ""} onClick={() => setTimelineSource("working")}>Working</button></div>
                <button className={styles.iconButton} type="button" title="Export plan" aria-label="Export plan" onClick={exportPlan}><Download size={17} /></button>
              </div>
            </div>
            <TimelineBoard caseData={caseData} plan={displayPlan} zoom={zoom} setZoom={setZoom} onSelect={setSelectedAssignment} onDropJob={dropJob} onSick={markSick} changedTechnicians={changedTechnicians} editable={timelineSource === "working" && !busyAction} />
          </div>

          <aside className={styles.unassignedPanel} aria-label="Unassigned jobs">
            <div className={styles.unassignedHeader}><div><span className={styles.kicker}>Backend result</span><h2>Unassigned jobs</h2></div><span>{displayPlan.unassigned.length}</span></div>
            {displayPlan.unassigned.length === 0 ? <div className={styles.zeroState}><BadgeCheck size={25} /><strong>0 unassigned</strong><p>Every job has a confirmed route.</p></div> : <div className={styles.unassignedList}>{displayPlan.unassigned.map((item) => <article key={item.job_id}><div><span className={`${styles.skillMark} ${styles[`skill_${item.skill}`]}`}>{skillIcon(item.skill, 14)}</span><span><strong>{item.job_id}</strong><small>{item.area} / {formatSkill(item.skill)}</small></span><time>{item.window_start}-{item.window_end}</time></div><p><CircleAlert size={13} />{item.reason_text}</p></article>)}</div>}
          </aside>
        </section>
      )}

      {activeView === "setup" && (
        <section className={styles.setupView}>
          <div className={styles.viewTitle}><div><span className={styles.kicker}>{setupCopy.kicker}</span><h1>{setupCopy.title}</h1><p>{setupCopy.description}</p></div><div>{hasUnsavedChanges ? <span className={styles.changeStatus}><CircleAlert size={14} /> Unsaved changes</span> : planNeedsRefresh ? <span className={styles.changeStatus}><CircleAlert size={14} /> Update needed</span> : <span className={styles.savedStatus}><Check size={14} /> Saved</span>}<button className={styles.primaryButton} type="button" onClick={saveSetup} disabled={!!busyAction || !hasUnsavedChanges}>{busyAction === "save" ? <LoaderCircle className={styles.spinner} size={16} /> : <Check size={16} />} Save and update plan</button></div></div>

          {setupTab === "technicians" && <div className={styles.dataPanel}><div className={styles.dataToolbar}><div><h2>Technicians</h2><p>Skills and shift availability for {formatDate(caseData.today)}.</p></div><button className={styles.secondaryButton} type="button" onClick={() => setSetupModal("technician")}><Plus size={16} /> Add technician</button></div><div className={styles.tableScroll}><table><thead><tr><th>ID</th><th>Name</th><th>Skills</th><th>Shift start</th><th>Shift end</th><th>Home area</th><th>Status</th></tr></thead><tbody>{caseData.technicians.map((technician, index) => <tr key={technician.id}><td><code>{technician.id}</code></td><td><input value={technician.name} onChange={(event) => updateTechnician(index, "name", event.target.value)} aria-label={`${technician.id} name`} /></td><td><input value={technician.skills.join(", ")} onChange={(event) => updateTechnician(index, "skills", event.target.value.split(",").map((item) => item.trim()).filter(Boolean))} aria-label={`${technician.id} skills`} /></td><td><input type="time" value={technician.shift_start} onChange={(event) => updateTechnician(index, "shift_start", event.target.value)} aria-label={`${technician.id} shift start`} /></td><td><input type="time" value={technician.shift_end} onChange={(event) => updateTechnician(index, "shift_end", event.target.value)} aria-label={`${technician.id} shift end`} /></td><td><select value={technician.home_area} onChange={(event) => updateTechnician(index, "home_area", event.target.value)} aria-label={`${technician.id} home area`}>{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></td><td><select value={technician.status ?? "active"} onChange={(event) => updateTechnician(index, "status", event.target.value)} aria-label={`${technician.id} status`}><option value="active">Active</option><option value="sick">Sick</option></select></td></tr>)}</tbody></table></div></div>}

          {setupTab === "jobs" && <div className={styles.dataPanel}><div className={styles.dataToolbar}><div><h2>Jobs</h2><p>{caseData.jobs.length} service requests.</p></div><div className={styles.tableActions}><label className={styles.searchField}><Search size={15} /><input value={jobSearch} onChange={(event) => setJobSearch(event.target.value)} placeholder="Search jobs" aria-label="Search jobs" /></label><button className={styles.secondaryButton} type="button" onClick={() => setSetupModal("job")}><Plus size={16} /> Add job</button></div></div><div className={styles.tableScroll}><table><thead><tr><th>ID</th><th>Area</th><th>Required skill</th><th>Duration</th><th>Window start</th><th>Window end</th><th>Status</th></tr></thead><tbody>{filteredJobs.map((job) => { const index = caseData.jobs.findIndex((item) => item.id === job.id); return <tr key={job.id}><td><code>{job.id}</code></td><td><select value={job.area} onChange={(event) => updateJob(index, "area", event.target.value)} aria-label={`${job.id} area`}>{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></td><td><select value={job.skill} onChange={(event) => updateJob(index, "skill", event.target.value)} aria-label={`${job.id} skill`}><option value="electrical">Electrical</option><option value="plumbing">Plumbing</option><option value="ac">AC</option><option value="gas_line">Gas line</option></select></td><td><input type="number" min="15" step="15" value={job.duration_minutes} onChange={(event) => updateJob(index, "duration_minutes", Number(event.target.value))} aria-label={`${job.id} duration`} /></td><td><input type="time" value={job.window_start} onChange={(event) => updateJob(index, "window_start", event.target.value)} aria-label={`${job.id} window start`} /></td><td><input type="time" value={job.window_end} onChange={(event) => updateJob(index, "window_end", event.target.value)} aria-label={`${job.id} window end`} /></td><td><select value={job.status ?? "ready"} onChange={(event) => updateJob(index, "status", event.target.value)} aria-label={`${job.id} status`}><option value="ready">Pending</option><option value="in_progress">In progress</option><option value="done">Done</option></select></td></tr>; })}</tbody></table></div></div>}

          {setupTab === "matrix" && <div className={styles.dataPanel}><div className={styles.dataToolbar}><div><h2>Travel times</h2><p>Choose two areas and enter the drive time once.</p></div></div><div className={styles.matrixEditor}><label><span>From</span><select value={matrixFrom} onChange={(event) => setMatrixFrom(event.target.value)} aria-label="From area">{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></label><ArrowRight size={18} /><label><span>To</span><select value={matrixTo} onChange={(event) => setMatrixTo(event.target.value)} aria-label="To area">{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></label><label><span>Minutes</span><input type="number" min="0" step="1" value={caseData.travel_minutes[matrixFrom]?.[matrixTo] ?? 0} onChange={(event) => { if (event.target.value) updateTravelTime(matrixFrom, matrixTo, Number(event.target.value)); }} aria-label="Travel minutes" /></label></div><div className={styles.matrixScroll}><table className={styles.matrix}><thead><tr><th>From / to</th>{caseData.areas.map((area) => <th key={area}>{area}</th>)}</tr></thead><tbody>{caseData.areas.map((from) => <tr key={from}><th>{from}</th>{caseData.areas.map((to) => <td className={from === to ? styles.matrixSame : ""} key={to}>{caseData.travel_minutes[from][to]}<small>min</small></td>)}</tr>)}</tbody></table></div></div>}
        </section>
      )}

      {activeView === "analytics" && (
        <section className={styles.analyticsView}>
          <div className={styles.viewTitle}>
            <div><span className={styles.kicker}>Working plan / {formatDate(caseData.today)}</span><h1>Dispatch analytics</h1><p>Coverage, capacity, travel, and workload for the selected operating date.</p></div>
            <button className={styles.primaryButton} type="button" onClick={() => openView("plan")}>Open timeline <ArrowRight size={16} /></button>
          </div>

          <div className={styles.analyticsMetrics}>
            <article><span><BadgeCheck size={18} /></span><div><small>Job coverage</small><strong>{analytics.coverage}%</strong><p>{plan.stats.jobs_scheduled} of {caseData.jobs.length} scheduled</p></div></article>
            <article><span><Gauge size={18} /></span><div><small>Team utilization</small><strong>{analytics.utilization}%</strong><p>Service and travel vs. active shifts</p></div></article>
            <article><span><CarFront size={18} /></span><div><small>Travel per job</small><strong>{analytics.travelPerJob}m</strong><p>{plan.stats.total_travel_minutes} minutes total</p></div></article>
            <article><span><TrendingDown size={18} /></span><div><small>Baseline difference</small><strong>{Math.abs(analytics.travelSaved)}m</strong><p>{analytics.travelSaved >= 0 ? "Less travel than first-fit" : "More travel than first-fit"}</p></div></article>
          </div>

          <div className={styles.analyticsSplit}>
            <section className={styles.analyticsPanel}>
              <div className={styles.analyticsHeader}><span><MapPinned size={18} /></span><div><h2>Demand by area</h2><p>Scheduled share of jobs received.</p></div></div>
              <div className={styles.areaRows}>{analytics.areaRows.map((item) => <div key={item.area}><span><strong>{item.area}</strong><small>{item.scheduled}/{item.total} scheduled</small></span><div><i style={{ width: `${(item.total / analytics.maxAreaJobs) * 100}%` }}><b style={{ width: `${item.total ? (item.scheduled / item.total) * 100 : 0}%` }} /></i></div></div>)}</div>
            </section>

            <section className={styles.analyticsPanel}>
              <div className={styles.analyticsHeader}><span><Wrench size={18} /></span><div><h2>Skill capacity</h2><p>Demand compared with active coverage.</p></div></div>
              <div className={styles.skillRows}>{analytics.skillRows.map((item) => <div key={item.skill}><span className={`${styles.skillMark} ${styles[`skill_${item.skill}`]}`}>{skillIcon(item.skill, 15)}</span><span><strong>{formatSkill(item.skill)}</strong><small>{item.jobs} jobs</small></span><b>{item.technicians}</b><em>technicians</em></div>)}</div>
            </section>
          </div>

          <section className={styles.workloadPanel}>
            <div className={styles.analyticsHeader}><span><BarChart3 size={18} /></span><div><h2>Technician workload</h2><p>Working-plan utilization, job count, and travel.</p></div></div>
            <div className={styles.workloadRows}>{analytics.workloads.map((item) => <div key={item.technician.id}><span><strong>{item.technician.name}</strong><small>{item.technician.id} / {item.technician.status === "sick" ? "Unavailable" : item.technician.shift_start + "-" + item.technician.shift_end}</small></span><div><i style={{ width: `${item.utilization}%` }} /></div><b>{item.utilization}%</b><em>{item.jobs} jobs / {item.travel}m travel</em></div>)}</div>
          </section>
        </section>
      )}

      {activeView === "compare" && (
        <section className={styles.compareView}>
          <div className={styles.viewTitle}><div><span className={styles.kicker}>Plan comparison</span><h1>Baseline vs. working plan</h1><p>Compare first-fit routing with the optimized plan and manual changes.</p></div></div>
          <div className={styles.comparisonGrid}>
            {[{ id: "baseline" as const, label: "First-fit baseline", data: baselinePlan }, { id: "working" as const, label: "Optimized / working", data: plan }].map((item) => <button type="button" className={timelineSource === item.id ? styles.comparisonActive : ""} key={item.id} onClick={() => setTimelineSource(item.id)}><span className={styles.comparisonTop}><span><small>{item.label}</small><strong>{item.data.score}<em>/100</em></strong></span>{timelineSource === item.id && <BadgeCheck size={20} />}</span><span className={styles.comparisonMetrics}><span><b>{item.data.stats.total_travel_minutes}m</b><small>Travel</small></span><span><b>{item.data.stats.jobs_scheduled}</b><small>Scheduled</small></span><span><b>{item.data.stats.jobs_unassigned}</b><small>Unassigned</small></span><span><b>{item.data.stats.jobs_at_risk}</b><small>At risk</small></span></span><span className={styles.openTimeline}>View this timeline <ArrowRight size={16} /></span></button>)}
          </div>
          <div className={styles.routeSummary}><div className={styles.dataToolbar}><div><h2>{timelineSource === "baseline" ? "Baseline" : "Working"} route load</h2><p>Jobs and travel by technician.</p></div><button className={styles.primaryButton} type="button" onClick={() => openView("plan")}>Open selected timeline <ArrowRight size={16} /></button></div><div className={styles.routeSummaryRows}>{caseData.technicians.map((technician) => { const route = displayPlan.assignments[technician.id] ?? []; const travel = route.reduce((sum, item) => sum + item.travel_minutes, 0); return <div key={technician.id}><span><strong>{technician.name}</strong><small>{technician.id}</small></span><div><i style={{ width: `${Math.min(100, route.length * 18)}%` }} /></div><b>{route.length} jobs</b><em>{travel}m travel</em></div>; })}</div></div>
        </section>
      )}
      </div>

      {setupModal === "technician" && (
        <div className={styles.drawerBackdrop} onMouseDown={(event) => { if (event.target === event.currentTarget) setSetupModal(null); }}>
          <form className={styles.setupModal} onSubmit={addTechnician} role="dialog" aria-modal="true" aria-labelledby="add-technician-title">
            <div className={styles.drawerHeader}><div><span className={styles.setupModalIcon}><UsersRound size={19} /></span><span><small>New team member</small><h2 id="add-technician-title">Add technician</h2></span></div><button className={styles.iconButton} type="button" title="Close" aria-label="Close add technician" onClick={() => setSetupModal(null)}><X size={18} /></button></div>
            <div className={styles.formGrid}><label className={styles.fullField}><span>Name</span><input name="name" placeholder="Technician name" required autoFocus /></label><label><span>Home area</span><select name="home_area" required>{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></label><fieldset className={styles.skillOptions}><legend>Skills</legend><label><input type="checkbox" name="skills" value="ac" defaultChecked /><Snowflake size={15} /> AC</label><label><input type="checkbox" name="skills" value="plumbing" /><Droplets size={15} /> Plumbing</label><label><input type="checkbox" name="skills" value="electrical" /><Zap size={15} /> Electrical</label><label><input type="checkbox" name="skills" value="gas_line" /><Flame size={15} /> Gas line</label></fieldset><label><span>Shift start</span><input type="time" name="shift_start" defaultValue="09:00" required /></label><label><span>Shift end</span><input type="time" name="shift_end" defaultValue="18:00" required /></label></div>
            <div className={styles.modalActions}><button className={styles.secondaryButton} type="button" onClick={() => setSetupModal(null)}>Cancel</button><button className={styles.primaryButton} type="submit"><Plus size={16} /> Add technician</button></div>
          </form>
        </div>
      )}

      {setupModal === "job" && (
        <div className={styles.drawerBackdrop} onMouseDown={(event) => { if (event.target === event.currentTarget) setSetupModal(null); }}>
          <form className={styles.setupModal} onSubmit={addJob} role="dialog" aria-modal="true" aria-labelledby="add-job-title">
            <div className={styles.drawerHeader}><div><span className={styles.setupModalIcon}><Wrench size={19} /></span><span><small>New service request</small><h2 id="add-job-title">Add job</h2></span></div><button className={styles.iconButton} type="button" title="Close" aria-label="Close add job" onClick={() => setSetupModal(null)}><X size={18} /></button></div>
            <div className={styles.formGrid}><label><span>Area</span><select name="area" required>{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></label><label><span>Required skill</span><select name="skill" required><option value="ac">AC</option><option value="plumbing">Plumbing</option><option value="electrical">Electrical</option><option value="gas_line">Gas line</option></select></label><label><span>Duration</span><select name="duration" defaultValue="60"><option value="30">30 min</option><option value="45">45 min</option><option value="60">60 min</option><option value="90">90 min</option><option value="120">120 min</option><option value="150">150 min</option></select></label><span /><label><span>Window start</span><input type="time" name="window_start" defaultValue="09:00" required /></label><label><span>Window end</span><input type="time" name="window_end" defaultValue="12:00" required /></label></div>
            <div className={styles.modalActions}><button className={styles.secondaryButton} type="button" onClick={() => setSetupModal(null)}>Cancel</button><button className={styles.primaryButton} type="submit"><Plus size={16} /> Add job</button></div>
          </form>
        </div>
      )}

      {selectedAssignment && (
        <div className={styles.drawerBackdrop} onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedAssignment(null); }}>
          <aside className={styles.jobDrawer} role="dialog" aria-modal="true" aria-labelledby="job-title">
            <div className={styles.drawerHeader}><div><span className={`${styles.skillMark} ${styles[`skill_${selectedAssignment.skill}`]}`}>{skillIcon(selectedAssignment.skill, 16)}</span><span><small>Scheduled job</small><h2 id="job-title">{selectedAssignment.job_id}</h2></span></div><button className={styles.iconButton} type="button" title="Close details" aria-label="Close details" onClick={() => setSelectedAssignment(null)}><X size={18} /></button></div>
            <div className={styles.jobLocation}><MapPin size={17} /><span><strong>{selectedAssignment.area}</strong><small>{formatSkill(selectedAssignment.skill)} service</small></span></div>
            <dl className={styles.jobFacts}><div><dt>Service</dt><dd>{selectedAssignment.start}-{selectedAssignment.end}</dd></div><div><dt>Customer window</dt><dd>{selectedAssignment.window_start}-{selectedAssignment.window_end}</dd></div><div><dt>Duration</dt><dd>{selectedAssignment.duration_minutes} min</dd></div><div><dt>Travel in</dt><dd>{selectedAssignment.travel_minutes} min</dd></div><div><dt>Assigned to</dt><dd>{caseData.technicians.find((item) => item.id === selectedAssignment.technician_id)?.name}</dd></div></dl>
            <div className={selectedAssignment.at_risk ? styles.marginRisk : styles.marginSafe}>{selectedAssignment.at_risk ? <AlertTriangle size={18} /> : <BadgeCheck size={18} />}<span><strong>{selectedAssignment.at_risk ? "Window at risk" : "Arrival on time"}</strong><small>{selectedAssignment.margin_minutes} minutes before window closes</small></span></div>
          </aside>
        </div>
      )}

      {showEmergency && (
        <div className={styles.drawerBackdrop} onMouseDown={(event) => { if (event.target === event.currentTarget) setShowEmergency(false); }}>
          <form className={styles.emergencyModal} onSubmit={addEmergency} role="dialog" aria-modal="true" aria-labelledby="emergency-title">
            <div className={styles.drawerHeader}><div><span className={styles.emergencyIcon}><Siren size={19} /></span><span><small>Active replan</small><h2 id="emergency-title">Add emergency job</h2></span></div><button className={styles.iconButton} type="button" title="Close" aria-label="Close emergency job" onClick={() => setShowEmergency(false)}><X size={18} /></button></div>
            <div className={styles.formGrid}><label><span>Area</span><select name="area" required>{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></label><label><span>Required skill</span><select name="skill" required><option value="electrical">Electrical</option><option value="plumbing">Plumbing</option><option value="ac">AC</option><option value="gas_line">Gas line</option></select></label><label><span>Duration</span><select name="duration" defaultValue="60"><option value="30">30 min</option><option value="45">45 min</option><option value="60">60 min</option><option value="90">90 min</option></select></label><label><span>Window start</span><input type="time" name="window_start" defaultValue="14:00" required /></label><label><span>Window end</span><input type="time" name="window_end" defaultValue="16:00" required /></label></div>
            <p className={styles.replanNote}><Route size={16} />Pending work will be replanned. Jobs already in progress remain locked.</p>
            <div className={styles.modalActions}><button className={styles.secondaryButton} type="button" onClick={() => setShowEmergency(false)}>Cancel</button><button className={styles.generateButton} type="submit" disabled={busyAction === "emergency"}>{busyAction === "emergency" ? <LoaderCircle className={styles.spinner} size={16} /> : <Siren size={16} />} Add and replan</button></div>
          </form>
        </div>
      )}

      {busyAction && busyAction !== "generate" && busyAction !== "case" && busyAction !== "save" && busyAction !== "emergency" && <div className={styles.movePending}><LoaderCircle className={styles.spinner} size={16} /> Validating {busyAction}</div>}
    </main>
  );
}
