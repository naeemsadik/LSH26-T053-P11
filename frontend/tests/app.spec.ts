import { expect, test } from "@playwright/test";

test("dispatcher plan renders and rejects an invalid move", async ({ page }) => {
  const errors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });
  page.on("pageerror", (error) => errors.push(error.message));

  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/");
  await expect(page.locator("html")).toHaveAttribute("data-routeboard-ready", "true");

  await expect(page).toHaveTitle(/Routeboard/);
  await expect(page.getByRole("heading", { name: "PUB-01 day plan" })).toBeVisible();
  await expect(page.locator("[class*='technicianRow']")).toHaveCount(12);
  expect(await page.locator("[class*='jobBlock']").count()).toBeGreaterThan(0);
  await expect(page.getByRole("complementary", { name: "Unassigned jobs" })).toBeVisible();

  await page.screenshot({ path: "test-results/routeboard-plan.png", fullPage: true });

  await page.locator("[class*='jobBlock']").first().click();
  await expect(page.getByRole("dialog", { name: /J\d+/ })).toBeVisible();
  await page.getByRole("button", { name: "Close details" }).click();

  const electricalJob = page.locator("[class*='timelinePanel'] [class*='jobBlock'][class*='skill_electrical']").first();
  const plumbingOnlyLane = page.locator("[class*='technicianRow']").filter({ hasText: "T09" }).locator("[class*='lane']");
  await electricalJob.dragTo(plumbingOnlyLane);
  await expect(page.getByText(/Skill mismatch:/)).toBeVisible();

  await page.getByRole("button", { name: "Dismiss message" }).click();
  await page.getByRole("button", { name: "Emergency job" }).click();
  await page.getByRole("button", { name: "Add and replan" }).click();
  await expect(page.getByText(/added; active routes replanned/)).toBeVisible();

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Mark Rafiq sick" }).click();
  await expect(page.getByText(/Rafiq marked unavailable/)).toBeVisible();
  await expect(page.getByText("Unavailable", { exact: true }).first()).toBeVisible();

  await page.reload();
  await expect(page.getByText("Unavailable", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("E01", { exact: true }).first()).toBeVisible();

  expect(errors).toEqual([]);
});

test("setup, matrix, comparison, and case switching work", async ({ page }) => {
  await page.setViewportSize({ width: 1366, height: 850 });
  await page.goto("/");
  await expect(page.locator("html")).toHaveAttribute("data-routeboard-ready", "true");

  await page.getByRole("button", { name: "Setup" }).click();
  await expect(page.getByRole("heading", { name: "Case setup" })).toBeVisible();
  await expect(page).toHaveURL(/#setup\/technicians$/);
  await expect(page.locator("tbody tr")).toHaveCount(12);
  await page.getByLabel("T01 name").fill("Updated technician");
  await expect(page.getByText("Unsaved changes")).toBeVisible();
  await page.getByRole("button", { name: "Add technician" }).click();
  await page.getByRole("dialog", { name: "Add technician" }).getByLabel("Name").fill("New field technician");
  await page.getByRole("dialog", { name: "Add technician" }).getByRole("button", { name: "Add technician", exact: true }).click();
  await expect(page.getByLabel("T13 name")).toHaveValue("New field technician");

  await page.getByRole("button", { name: /Jobs/ }).click();
  await expect(page).toHaveURL(/#setup\/jobs$/);
  await expect(page.getByLabel("Search jobs")).toBeVisible();
  await page.getByLabel("Search jobs").fill("gas line");
  await expect(page.getByText("J21", { exact: true })).toBeVisible();
  await page.getByLabel("Search jobs").fill("");
  await page.getByRole("button", { name: "Add job" }).click();
  await page.getByRole("dialog", { name: "Add job" }).getByRole("button", { name: "Add job", exact: true }).click();
  await expect(page.getByText("J38", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: /Travel times/ }).click();
  await expect(page).toHaveURL(/#setup\/matrix$/);
  await expect(page.getByRole("heading", { name: "Travel times" })).toBeVisible();
  await page.getByLabel("From area").selectOption("Banani");
  await page.getByLabel("To area").selectOption("Bashundhara");
  await page.getByLabel("Travel minutes").fill("21");
  await page.getByRole("button", { name: "Save and update plan" }).click();
  await expect(page.getByText("Saved", { exact: true })).toBeVisible();

  await page.reload();
  await expect(page.getByRole("heading", { name: "Travel times" })).toBeVisible();
  await expect(page).toHaveURL(/#setup\/matrix$/);
  await page.getByLabel("From area").selectOption("Banani");
  await page.getByLabel("To area").selectOption("Bashundhara");
  await expect(page.getByLabel("Travel minutes")).toHaveValue("21");
  await page.getByRole("button", { name: /Technicians/ }).click();
  await expect(page.getByLabel("T13 name")).toHaveValue("New field technician");

  await page.getByRole("button", { name: "Compare" }).click();
  await expect(page.getByRole("heading", { name: "Baseline vs. working plan" })).toBeVisible();
  await expect(page.getByRole("button", { name: /First-fit baseline/ })).toBeVisible();
  await expect(page.getByRole("button", { name: /Optimized \/ working/ })).toBeVisible();

  await page.locator("select").first().selectOption("PUB-02");
  await page.getByRole("navigation", { name: "Workspace views" }).getByRole("button", { name: "Plan", exact: true }).click();
  await expect(page.getByRole("heading", { name: "PUB-02 day plan" })).toBeVisible();
  await expect(page.locator("[class*='technicianRow']")).toHaveCount(15);
});

test("planning API returns explicit rule validation", async ({ request }) => {
  const caseResponse = await request.get("/api/cases/PUB-01");
  expect(caseResponse.ok()).toBeTruthy();
  const caseData = await caseResponse.json();

  const planResponse = await request.post("/api/plan/generate", {
    data: { case_id: "PUB-01", case_data: caseData },
  });
  expect(planResponse.ok()).toBeTruthy();
  const plan = await planResponse.json();
  expect(plan.stats.jobs_scheduled + plan.stats.jobs_unassigned).toBe(caseData.jobs.length);
  expect(plan.unassigned.every((item: { reason_text?: string }) => Boolean(item.reason_text))).toBeTruthy();

  const baselineResponse = await request.post("/api/plan/baseline", {
    data: { case_id: "PUB-01", case_data: caseData },
  });
  expect(baselineResponse.ok()).toBeTruthy();
  const baseline = await baselineResponse.json();
  expect(baseline.stats.jobs_scheduled + baseline.stats.jobs_unassigned).toBe(caseData.jobs.length);

  const electricalJob = Object.values(plan.assignments)
    .flat()
    .find((item) => (item as { skill: string }).skill === "electrical") as { job_id: string };
  const validationResponse = await request.post("/api/plan/validate-move", {
    data: {
      case_id: "PUB-01",
      case_data: caseData,
      plan,
      job_id: electricalJob.job_id,
      to_technician: "T09",
      desired_start: "13:00",
    },
  });
  expect(validationResponse.ok()).toBeTruthy();
  await expect(validationResponse.json()).resolves.toMatchObject({
    valid: false,
    reason_code: "SKILL_MISMATCH",
  });
});

test("all public cases produce hard-rule-safe plan partitions", async ({ request }) => {
  const minutes = (value: string) => {
    const [hours, minute] = value.split(":").map(Number);
    return hours * 60 + minute;
  };

  await Promise.all(Array.from({ length: 25 }, async (_, index) => {
    const caseId = `PUB-${String(index + 1).padStart(2, "0")}`;
    const caseResponse = await request.get(`/api/cases/${caseId}`);
    expect(caseResponse.ok()).toBeTruthy();
    const caseData = await caseResponse.json();
    const planResponse = await request.post("/api/plan/generate", {
      data: { case_id: caseId, case_data: caseData },
    });
    expect(planResponse.ok()).toBeTruthy();
    const plan = await planResponse.json();
    const scheduled = Object.values(plan.assignments).flat() as Array<{
      job_id: string;
      technician_id: string;
      skill: string;
      start: string;
      end: string;
      window_start: string;
      window_end: string;
    }>;
    const ids = [...scheduled.map((item) => item.job_id), ...plan.unassigned.map((item: { job_id: string }) => item.job_id)];

    expect(new Set(ids).size).toBe(caseData.jobs.length);
    expect(ids).toHaveLength(caseData.jobs.length);
    expect(plan.unassigned.every((item: { reason_text?: string }) => Boolean(item.reason_text))).toBeTruthy();

    for (const assignment of scheduled) {
      const technician = caseData.technicians.find((item: { id: string }) => item.id === assignment.technician_id);
      expect(technician.skills).toContain(assignment.skill);
      expect(minutes(assignment.start)).toBeGreaterThanOrEqual(minutes(assignment.window_start));
      expect(minutes(assignment.start)).toBeLessThanOrEqual(minutes(assignment.window_end));
      expect(minutes(assignment.start)).toBeGreaterThanOrEqual(minutes(technician.shift_start));
      expect(minutes(assignment.end)).toBeLessThanOrEqual(minutes(technician.shift_end));
    }

    for (const route of Object.values(plan.assignments) as Array<Array<{ start: string; end: string }>>) {
      const sorted = [...route].sort((a, b) => minutes(a.start) - minutes(b.start));
      for (let item = 1; item < sorted.length; item += 1) {
        expect(minutes(sorted[item].start)).toBeGreaterThanOrEqual(minutes(sorted[item - 1].end));
      }
    }
  }));
});
