import { apiFetch } from "../api/client";
import type { IntelligenceJobRequest, IntelligenceJobResponse } from "../../types/intelligence";

export async function createIntelligenceJob(
  request: IntelligenceJobRequest,
): Promise<IntelligenceJobResponse> {
  const correlationId = crypto.randomUUID();
  const response = await apiFetch("/api/intelligence/jobs", {
    method: "POST",
    headers: {
      "X-Correlation-Id": correlationId,
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error(`Intelligence job failed (${response.status})`);
  }

  return (await response.json()) as IntelligenceJobResponse;
}
