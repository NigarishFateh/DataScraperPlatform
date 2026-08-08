import { Spinner } from "../ui/Spinner";
import type { JobPhase, JobStatus } from "../../types/job";

type JobPhaseIndicatorProps = {
  phase: JobPhase;
  status: JobStatus;
  discoveredCount: number;
  enrichedCount: number;
};

type StepState = "pending" | "active" | "complete";

const PHASE_ORDER: JobPhase[] = [
  "CREATED",
  "DISCOVERY",
  "ENRICHMENT",
  "AGGREGATION",
  "NORMALIZATION",
  "VALIDATION",
  "PERSISTENCE",
  "EXPORT",
  "DONE",
];

const STEPS = [
  {
    phase: "DISCOVERY" as JobPhase,
    label: "Discovery",
    activeHint: "Searching for companies…",
    doneHint: "Companies found",
    count: (discovered: number) => discovered,
    countLabel: (count: number) =>
      count === 1 ? "1 company found" : `${count.toLocaleString()} companies found`,
  },
  {
    phase: "ENRICHMENT" as JobPhase,
    label: "Enrichment",
    activeHint: "Gathering contact details…",
    doneHint: "Details collected",
    count: (_discovered: number, enriched: number) => enriched,
    countLabel: (count: number) =>
      count === 1 ? "1 company enriched" : `${count.toLocaleString()} companies enriched`,
  },
] as const;

function phaseRank(phase: JobPhase): number {
  const index = PHASE_ORDER.indexOf(phase);
  return index >= 0 ? index : 0;
}

function stepState(
  stepPhase: JobPhase,
  currentPhase: JobPhase,
  status: JobStatus
): StepState {
  const stepRank = phaseRank(stepPhase);
  const currentRank = phaseRank(currentPhase);

  if (status === "COMPLETED" || currentRank > stepRank) {
    return "complete";
  }

  if (status === "FAILED" && currentPhase === stepPhase) {
    return "active";
  }

  if (
    currentPhase === stepPhase &&
    (status === "RUNNING" || status === "QUEUED" || status === "PAUSED")
  ) {
    return "active";
  }

  return "pending";
}

function StepIcon({ state }: { state: StepState }) {
  if (state === "active") {
    return <Spinner size="sm" />;
  }

  if (state === "complete") {
    return (
      <span className="flex h-3.5 w-3.5 items-center justify-center rounded-full bg-signal/20 text-[9px] font-bold text-signal">
        ✓
      </span>
    );
  }

  return <span className="h-3.5 w-3.5 rounded-full border border-white/15 bg-ink-900/60" />;
}

export function JobPhaseIndicator({
  phase,
  status,
  discoveredCount,
  enrichedCount,
}: JobPhaseIndicatorProps) {
  const isProcessingLater =
    status === "RUNNING" &&
    phaseRank(phase) > phaseRank("ENRICHMENT") &&
    !isTerminalLike(status);

  return (
    <div className="space-y-3">
      <div className="flex items-stretch gap-2">
        {STEPS.map((step, index) => {
          const state = stepState(step.phase, phase, status);
          const count = step.count(discoveredCount, enrichedCount);
          const isActive = state === "active";
          const isComplete = state === "complete";

          return (
            <div key={step.phase} className="flex min-w-0 flex-1 items-stretch gap-2">
              <div
                className={`flex min-w-0 flex-1 flex-col gap-2 rounded-lg border px-3 py-2.5 transition-all duration-500 ${
                  isActive
                    ? "border-signal/40 bg-signal/10 shadow-[0_0_24px_rgba(61,206,167,0.12)]"
                    : isComplete
                      ? "border-signal/20 bg-signal/5"
                      : "border-white/8 bg-ink-900/40"
                }`}
              >
                <div className="flex items-center gap-2">
                  <StepIcon state={state} />
                  <span
                    className={`font-display text-xs font-semibold ${
                      isActive ? "text-signal" : isComplete ? "text-mist-100" : "text-mist-400"
                    }`}
                  >
                    {step.label}
                  </span>
                  {isActive && status === "RUNNING" ? (
                    <span className="ml-auto flex gap-0.5">
                      <span className="h-1 w-1 animate-pulse rounded-full bg-signal [animation-delay:0ms]" />
                      <span className="h-1 w-1 animate-pulse rounded-full bg-signal [animation-delay:150ms]" />
                      <span className="h-1 w-1 animate-pulse rounded-full bg-signal [animation-delay:300ms]" />
                    </span>
                  ) : null}
                </div>

                <p
                  className={`text-[11px] leading-snug ${
                    isActive ? "text-mist-200" : "text-mist-400"
                  }`}
                >
                  {isActive
                    ? step.activeHint
                    : isComplete
                      ? step.doneHint
                      : "Waiting"}
                </p>

                <p
                  className={`font-medium tabular-nums ${
                    isActive ? "animate-pulse text-lg text-signal" : "text-sm text-mist-300"
                  }`}
                >
                  {count > 0
                    ? step.countLabel(count)
                    : isActive
                      ? "Counting…"
                      : "0"}
                </p>
              </div>

              {index < STEPS.length - 1 ? (
                <div className="flex w-4 shrink-0 items-center justify-center self-center">
                  <div
                    className={`h-px w-full transition-colors duration-500 ${
                      stepState(STEPS[index + 1].phase, phase, status) !== "pending"
                        ? "bg-signal/50"
                        : "bg-white/10"
                    }`}
                  />
                </div>
              ) : null}
            </div>
          );
        })}
      </div>

      {isProcessingLater ? (
        <div className="flex items-center gap-2 rounded-lg border border-white/8 bg-ink-900/40 px-3 py-2 text-[11px] text-mist-300">
          <Spinner size="sm" />
          <span>Finalizing results ({phase.replace(/_/g, " ").toLowerCase()})…</span>
        </div>
      ) : null}
    </div>
  );
}

function isTerminalLike(status: JobStatus): boolean {
  return status === "COMPLETED" || status === "FAILED" || status === "CANCELLED";
}
