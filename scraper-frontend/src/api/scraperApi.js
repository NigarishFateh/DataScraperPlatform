const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

export async function checkHealth() {
  const response = await fetch(`${API_BASE}/api/health`);
  if (!response.ok) {
    throw new Error('Orchestrator health check failed');
  }
  return response.json();
}

export async function runScrape({ sources, categories }) {
  const response = await fetch(`${API_BASE}/api/scrape`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ sources, categories }),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.detail || data.title || 'Scrape request failed');
  }

  return data;
}
