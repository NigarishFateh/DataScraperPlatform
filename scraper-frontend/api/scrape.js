/**
 * Vercel serverless proxy → Orchestrator scrape endpoint.
 * Scraping can take time; maxDuration is set in vercel.json (60s on Pro).
 */
export default async function handler(request, response) {
  if (request.method !== 'POST') {
    return response.status(405).json({
      title: 'Method Not Allowed',
      detail: 'Use POST for /api/scrape',
    });
  }

  const orchestratorUrl = process.env.ORCHESTRATOR_URL;

  if (!orchestratorUrl) {
    return response.status(500).json({
      title: 'Configuration Error',
      detail: 'ORCHESTRATOR_URL is not configured on Vercel.',
    });
  }

  try {
    const upstream = await fetch(`${orchestratorUrl}/api/scrape`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request.body ?? {}),
    });

    const body = await upstream.text();
    response.status(upstream.status);
    response.setHeader('Content-Type', upstream.headers.get('content-type') ?? 'application/json');
    return response.send(body);
  } catch (error) {
    return response.status(502).json({
      title: 'Upstream Error',
      detail: `Orchestrator scrape failed: ${error.message}`,
    });
  }
}
