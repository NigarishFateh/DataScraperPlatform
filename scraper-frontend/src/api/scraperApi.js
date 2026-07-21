const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

function buildUrl(path) {
  return `${API_BASE}${path}`;
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') ?? '';
  const isJson = contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const message =
      typeof data === 'object' && data !== null
        ? data.detail || data.title || data.message || 'Request failed'
        : String(data);
    throw new Error(message);
  }

  return data;
}

export async function checkHealth() {
  const response = await fetch(buildUrl('/api/health'));
  return parseResponse(response);
}

export async function runScrape({ sources, categories }) {
  const response = await fetch(buildUrl('/api/scrape'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ sources, categories }),
  });

  return parseResponse(response);
}

export function getApiMode() {
  if (API_BASE) {
    return `Direct → ${API_BASE}`;
  }
  if (import.meta.env.PROD) {
    return 'Vercel proxy → ORCHESTRATOR_URL';
  }
  return 'Local Vite proxy → localhost:8080';
}
