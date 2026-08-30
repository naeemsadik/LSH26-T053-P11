"use client";

import {
  AlertTriangle,
  ArrowRight,
  BadgeCheck,
  CalendarDays,
  CarFront,
  Check,
  CircleAlert,
  Clock3,
  Database,
  Download,
  Droplets,
  Flame,
  GitCompareArrows,
  GripVertical,
  LayoutDashboard,
  LoaderCircle,
  MapPin,
  Plus,
  Route,
  Search,
  Settings2,
  Siren,
  Snowflake,
  Table2,
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

type View = "plan" | "setup" | "compare";
type SetupTab = "technicians" | "jobs" | "matrix";
type TimelineSource = "generated" | "working";

const API_ROOT = (process.env.NEXT_PUBLIC_API_URL ?? "/api").replace(/\/$/, "");
const DAY_START = 8 * 60;
const DAY_END = 21 * 60;
const DAY_SPAN = DAY_END - DAY_START;

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
    weekday: "short",
    day: "2-digit",
    month: "short",
    year: "numeric",
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
                  <span><strong>{technician.name}</strong><small>{technician.id} · {technician.shift_start}-{technician.shift_end}</small></span>
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

export default function Dispatcher({ initialCase, initialPlan, cases }: { initialCase: CaseData; initialPlan: Plan; cases: CaseSummary[] }) {
  const [activeView, setActiveView] = useState<View>("plan");
  const [setupTab, setSetupTab] = useState<SetupTab>("technicians");
  const [caseData, setCaseData] = useState(initialCase);
  const [plan, setPlan] = useState(initialPlan);
  const [baselinePlan, setBaselinePlan] = useState(initialPlan);
  const [timelineSource, setTimelineSource] = useState<TimelineSource>("working");
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);
  const [zoom, setZoom] = useState(1);
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [validationError, setValidationError] = useState("");
  const [changedTechnicians, setChangedTechnicians] = useState<Set<string>>(new Set());
  const [showEmergency, setShowEmergency] = useState(false);
  const [jobSearch, setJobSearch] = useState("");

  useEffect(() => {
    document.documentElement.dataset.routeboardReady = "true";
    return () => { delete document.documentElement.dataset.routeboardReady; };
  }, []);

  const displayPlan = timelineSource === "generated" ? baselinePlan : plan;
  const filteredJobs = useMemo(() => {
    const query = jobSearch.trim().toLowerCase();
    return query
      ? caseData.jobs.filter((job) => `${job.id} ${job.area} ${formatSkill(job.skill)}`.toLowerCase().includes(query))
      : caseData.jobs;
  }, [caseData.jobs, jobSearch]);

  const notify = (message: string) => {
    setNotice(message);
    window.setTimeout(() => setNotice(""), 2800);
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
      setBaselinePlan(nextPlan);
      setTimelineSource("working");
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
      setCaseData(nextCase);
      setJobSearch("");
      const nextPlan = await requestJson<Plan>("/plan/generate", {
        method: "POST",
        body: JSON.stringify({ case_id: nextCase.case_id }),
      });
      setPlan(nextPlan);
      setBaselinePlan(nextPlan);
      setTimelineSource("working");
      setSelectedAssignment(null);
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
      setPlan(nextPlan);
      setTimelineSource("working");
      setChangedTechnicians(new Set([technician.id]));
      notify(`${technician.name} marked unavailable; pending jobs moved to unassigned.`);
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
      await requestJson(`/cases/${caseData.case_id}`, {
        method: "POST",
        body: JSON.stringify(caseData),
      });
      notify("Setup saved for this dispatcher session.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Setup could not be saved.");
    } finally {
      setBusyAction("");
    }
  };

  const updateTechnician = (index: number, field: keyof Technician, value: string | string[]) => {
    setCaseData((current) => ({
      ...current,
      technicians: current.technicians.map((technician, row) => row === index ? { ...technician, [field]: value } : technician),
    }));
  };

  const updateJob = (index: number, field: keyof Job, value: string | number) => {
    const jobId = filteredJobs[index]?.id;
    setCaseData((current) => ({
      ...current,
      jobs: current.jobs.map((job) => job.id === jobId ? { ...job, [field]: value } : job),
    }));
  };

  const addTechnician = () => {
    const number = caseData.technicians.length + 1;
    setCaseData((current) => ({
      ...current,
      technicians: [...current.technicians, {
        id: `T${String(number).padStart(2, "0")}`,
        name: "New technician",
        skills: ["electrical"],
        shift_start: "09:00",
        shift_end: "18:00",
        home_area: current.areas[0],
      }],
    }));
  };

  const addJob = () => {
    const number = caseData.jobs.length + 1;
    setCaseData((current) => ({
      ...current,
      jobs: [...current.jobs, {
        id: `J${String(number).padStart(2, "0")}`,
        area: current.areas[0],
        skill: "electrical",
        duration_minutes: 60,
        window_start: "09:00",
        window_end: "12:00",
      }],
    }));
  };

  const views = [
    { id: "plan" as const, label: "Plan", icon: LayoutDashboard },
    { id: "setup" as const, label: "Setup", icon: Database },
    { id: "compare" as const, label: "Compare", icon: GitCompareArrows },
  ];
  const stats = displayPlan.stats;

  return (
    <main className={styles.app}>
      <header className={styles.header}>
        <button className={styles.brand} type="button" onClick={() => setActiveView("plan")} aria-label="Routeboard home">
          <span><Route size={23} /></span>
          <strong>Routeboard<small>Dispatch optimiser</small></strong>
        </button>

        <nav className={styles.mainNav} aria-label="Workspace views">
          {views.map((view) => { const Icon = view.icon; return <button type="button" key={view.id} className={activeView === view.id ? styles.navActive : ""} onClick={() => setActiveView(view.id)}><Icon size={16} />{view.label}</button>; })}
        </nav>

        <div className={styles.headerActions}>
          <label className={styles.caseSelect}><span>Case</span><select value={caseData.case_id} onChange={(event) => selectCase(event.target.value)} disabled={!!busyAction}>{cases.map((item) => <option key={item.case_id} value={item.case_id}>{item.case_id} · {item.technicians} tech · {item.jobs} jobs</option>)}</select></label>
          <span className={styles.date}><CalendarDays size={15} />{formatDate(caseData.today)}</span>
          <span className={styles.apiStatus}><i />Demo API</span>
          <button className={styles.emergencyButton} type="button" onClick={() => setShowEmergency(true)}><Siren size={17} /> Emergency job</button>
          <button className={styles.generateButton} type="button" onClick={() => generate()} disabled={!!busyAction}>{busyAction === "generate" ? <LoaderCircle className={styles.spinner} size={17} /> : <Route size={17} />} Generate plan</button>
        </div>
      </header>

      {(activeView === "plan" || activeView === "compare") && (
        <section className={styles.statsBar} aria-label="Current plan statistics">
          <div className={styles.scoreStat}><span>Plan score</span><strong>{displayPlan.score}</strong><em>/100</em></div>
          <Stat icon={<CarFront size={18} />} label="Total travel" value={`${Math.floor(stats.total_travel_minutes / 60)}h ${stats.total_travel_minutes % 60}m`} note="Across all routes" />
          <Stat icon={<BadgeCheck size={18} />} label="Jobs scheduled" value={String(stats.jobs_scheduled)} note={`${caseData.jobs.length} jobs received`} />
          <Stat icon={<CircleAlert size={18} />} label="Unassigned" value={String(stats.jobs_unassigned)} note={stats.jobs_unassigned ? "Needs dispatcher action" : "All work covered"} alert={stats.jobs_unassigned > 0} />
          <Stat icon={<AlertTriangle size={18} />} label="At risk" value={String(stats.jobs_at_risk)} note="15 min margin or less" alert={stats.jobs_at_risk > 0} />
        </section>
      )}

      {(error || validationError || notice) && <div className={`${styles.banner} ${error || validationError ? styles.bannerError : styles.bannerSuccess}`} role="status">{error || validationError ? <CircleAlert size={16} /> : <Check size={16} />}<span>{error || validationError || notice}</span>{(error || validationError) && <button type="button" aria-label="Dismiss message" title="Dismiss" onClick={() => { setError(""); setValidationError(""); }}><X size={16} /></button>}</div>}

      {activeView === "plan" && (
        <section className={styles.planWorkspace}>
          <div className={styles.planMain}>
            <div className={styles.planToolbar}>
              <div><span className={styles.kicker}>Monday dispatch</span><h1>{caseData.case_id} day plan</h1><p>{caseData.technicians.length} technicians across {caseData.areas.length} Dhaka service areas</p></div>
              <div className={styles.toolbarRight}>
                <div className={styles.sourceToggle} aria-label="Timeline plan source"><button type="button" className={timelineSource === "generated" ? styles.sourceActive : ""} onClick={() => setTimelineSource("generated")}>Generated</button><button type="button" className={timelineSource === "working" ? styles.sourceActive : ""} onClick={() => setTimelineSource("working")}>Working</button></div>
                <button className={styles.iconButton} type="button" title="Export plan" aria-label="Export plan" onClick={() => notify("Plan export prepared.")}><Download size={17} /></button>
              </div>
            </div>
            <TimelineBoard caseData={caseData} plan={displayPlan} zoom={zoom} setZoom={setZoom} onSelect={setSelectedAssignment} onDropJob={dropJob} onSick={markSick} changedTechnicians={changedTechnicians} editable={timelineSource === "working" && !busyAction} />
          </div>

          <aside className={styles.unassignedPanel} aria-label="Unassigned jobs">
            <div className={styles.unassignedHeader}><div><span className={styles.kicker}>Backend result</span><h2>Unassigned jobs</h2></div><span>{displayPlan.unassigned.length}</span></div>
            {displayPlan.unassigned.length === 0 ? <div className={styles.zeroState}><BadgeCheck size={25} /><strong>0 unassigned</strong><p>Every job has a confirmed route.</p></div> : <div className={styles.unassignedList}>{displayPlan.unassigned.map((item) => <article key={item.job_id}><div><span className={`${styles.skillMark} ${styles[`skill_${item.skill}`]}`}>{skillIcon(item.skill, 14)}</span><span><strong>{item.job_id}</strong><small>{item.area} · {formatSkill(item.skill)}</small></span><time>{item.window_start}-{item.window_end}</time></div><p><CircleAlert size={13} />{item.reason_text}</p></article>)}</div>}
          </aside>
        </section>
      )}

      {activeView === "setup" && (
        <section className={styles.setupView}>
          <div className={styles.viewTitle}><div><span className={styles.kicker}>Input workspace</span><h1>Case setup</h1><p>Edit technicians, jobs, and travel data before generating a plan.</p></div><div><button className={styles.secondaryButton} type="button" onClick={() => generate()}><Route size={16} /> Generate from setup</button><button className={styles.primaryButton} type="button" onClick={saveSetup} disabled={busyAction === "save"}>{busyAction === "save" ? <LoaderCircle className={styles.spinner} size={16} /> : <Check size={16} />} Save setup</button></div></div>
          <div className={styles.setupTabs}>{(["technicians", "jobs", "matrix"] as SetupTab[]).map((tab) => <button type="button" className={setupTab === tab ? styles.setupTabActive : ""} onClick={() => setSetupTab(tab)} key={tab}>{tab === "technicians" ? <UsersRound size={16} /> : tab === "jobs" ? <Wrench size={16} /> : <Table2 size={16} />}{tab === "matrix" ? "Travel matrix" : tab[0].toUpperCase() + tab.slice(1)}<span>{tab === "technicians" ? caseData.technicians.length : tab === "jobs" ? caseData.jobs.length : caseData.areas.length}</span></button>)}</div>

          {setupTab === "technicians" && <div className={styles.dataPanel}><div className={styles.dataToolbar}><div><h2>Technicians</h2><p>Skills and shift availability for {caseData.today}.</p></div><button className={styles.secondaryButton} type="button" onClick={addTechnician}><Plus size={16} /> Add technician</button></div><div className={styles.tableScroll}><table><thead><tr><th>ID</th><th>Name</th><th>Skills</th><th>Shift start</th><th>Shift end</th><th>Home area</th><th>Status</th></tr></thead><tbody>{caseData.technicians.map((technician, index) => <tr key={technician.id}><td><code>{technician.id}</code></td><td><input value={technician.name} onChange={(event) => updateTechnician(index, "name", event.target.value)} aria-label={`${technician.id} name`} /></td><td><input value={technician.skills.join(", ")} onChange={(event) => updateTechnician(index, "skills", event.target.value.split(",").map((item) => item.trim()).filter(Boolean))} aria-label={`${technician.id} skills`} /></td><td><input type="time" value={technician.shift_start} onChange={(event) => updateTechnician(index, "shift_start", event.target.value)} aria-label={`${technician.id} shift start`} /></td><td><input type="time" value={technician.shift_end} onChange={(event) => updateTechnician(index, "shift_end", event.target.value)} aria-label={`${technician.id} shift end`} /></td><td><select value={technician.home_area} onChange={(event) => updateTechnician(index, "home_area", event.target.value)} aria-label={`${technician.id} home area`}>{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></td><td><span className={styles.activeStatus}><i /> Active</span></td></tr>)}</tbody></table></div></div>}

          {setupTab === "jobs" && <div className={styles.dataPanel}><div className={styles.dataToolbar}><div><h2>Jobs</h2><p>{caseData.jobs.length} service requests ready for assignment.</p></div><div className={styles.tableActions}><label className={styles.searchField}><Search size={15} /><input value={jobSearch} onChange={(event) => setJobSearch(event.target.value)} placeholder="Search jobs" aria-label="Search jobs" /></label><button className={styles.secondaryButton} type="button" onClick={addJob}><Plus size={16} /> Add job</button></div></div><div className={styles.tableScroll}><table><thead><tr><th>ID</th><th>Area</th><th>Required skill</th><th>Duration</th><th>Window start</th><th>Window end</th><th>Status</th></tr></thead><tbody>{filteredJobs.map((job, index) => <tr key={job.id}><td><code>{job.id}</code></td><td><select value={job.area} onChange={(event) => updateJob(index, "area", event.target.value)} aria-label={`${job.id} area`}>{caseData.areas.map((area) => <option key={area}>{area}</option>)}</select></td><td><select value={job.skill} onChange={(event) => updateJob(index, "skill", event.target.value)} aria-label={`${job.id} skill`}><option value="electrical">Electrical</option><option value="plumbing">Plumbing</option><option value="ac">AC</option><option value="gas_line">Gas line</option></select></td><td><input type="number" min="15" step="15" value={job.duration_minutes} onChange={(event) => updateJob(index, "duration_minutes", Number(event.target.value))} aria-label={`${job.id} duration`} /></td><td><input type="time" value={job.window_start} onChange={(event) => updateJob(index, "window_start", event.target.value)} aria-label={`${job.id} window start`} /></td><td><input type="time" value={job.window_end} onChange={(event) => updateJob(index, "window_end", event.target.value)} aria-label={`${job.id} window end`} /></td><td><span className={styles.readyStatus}><i /> Ready</span></td></tr>)}</tbody></table></div></div>}

          {setupTab === "matrix" && <div className={styles.dataPanel}><div className={styles.dataToolbar}><div><h2>Travel matrix</h2><p>Authoritative drive time in minutes. Diagonal values represent same-area travel.</p></div><span className={styles.readOnly}><Settings2 size={14} /> Read only</span></div><div className={styles.matrixScroll}><table className={styles.matrix}><thead><tr><th>From / to</th>{caseData.areas.map((area) => <th key={area}>{area}</th>)}</tr></thead><tbody>{caseData.areas.map((from) => <tr key={from}><th>{from}</th>{caseData.areas.map((to) => <td className={from === to ? styles.matrixSame : ""} key={to}>{caseData.travel_minutes[from][to]}<small> min</small></td>)}</tr>)}</tbody></table></div></div>}
        </section>
      )}

      {activeView === "compare" && (
        <section className={styles.compareView}>
          <div className={styles.viewTitle}><div><span className={styles.kicker}>Plan comparison</span><h1>Generated vs. working plan</h1><p>Review travel and coverage impact before confirming manual changes.</p></div></div>
          <div className={styles.comparisonGrid}>
            {[{ id: "generated" as const, label: "Generated plan", data: baselinePlan }, { id: "working" as const, label: "Working plan", data: plan }].map((item) => <button type="button" className={timelineSource === item.id ? styles.comparisonActive : ""} key={item.id} onClick={() => setTimelineSource(item.id)}><span className={styles.comparisonTop}><span><small>{item.label}</small><strong>{item.data.score}<em>/100</em></strong></span>{timelineSource === item.id && <BadgeCheck size={20} />}</span><span className={styles.comparisonMetrics}><span><b>{item.data.stats.total_travel_minutes}m</b><small>Travel</small></span><span><b>{item.data.stats.jobs_scheduled}</b><small>Scheduled</small></span><span><b>{item.data.stats.jobs_unassigned}</b><small>Unassigned</small></span><span><b>{item.data.stats.jobs_at_risk}</b><small>At risk</small></span></span><span className={styles.openTimeline}>View this timeline <ArrowRight size={16} /></span></button>)}
          </div>
          <div className={styles.routeSummary}><div className={styles.dataToolbar}><div><h2>{timelineSource === "generated" ? "Generated" : "Working"} route load</h2><p>Jobs and travel by technician.</p></div><button className={styles.primaryButton} type="button" onClick={() => setActiveView("plan")}>Open selected timeline <ArrowRight size={16} /></button></div><div className={styles.routeSummaryRows}>{caseData.technicians.map((technician) => { const route = displayPlan.assignments[technician.id] ?? []; const travel = route.reduce((sum, item) => sum + item.travel_minutes, 0); return <div key={technician.id}><span><strong>{technician.name}</strong><small>{technician.id}</small></span><div><i style={{ width: `${Math.min(100, route.length * 18)}%` }} /></div><b>{route.length} jobs</b><em>{travel}m travel</em></div>; })}</div></div>
        </section>
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
