/**
 * Vercel serverless proxy → Orchestrator health endpoint.
 * Set ORCHESTRATOR_URL in Vercel env vars (no trailing slash).
 */
export default async function handler(request, response) {
  const orchestratorUrl = process.env.ORCHESTRATOR_URL;

  if (!orchestratorUrl) {
    return response.status(500).json({
      service: 'scraper-orchestrator',
      status: 'DOWN',
      message: 'ORCHESTRATOR_URL is not configured on Vercel.',
    });
  }

  try {
    const upstream = await fetch(`${orchestratorUrl}/api/health`);

    const body = await upstream.text();
    response.status(upstream.status);
    response.setHeader('Content-Type', upstream.headers.get('content-type') ?? 'application/json');
    return response.send(body);
  } catch (error) {
    return response.status(502).json({
      service: 'scraper-orchestrator',
      status: 'DOWN',
      message: `Cannot reach orchestrator: ${error.message}`,
    });
  }
}
