/**
 * Vercel serverless proxy → Orchestrator scrape endpoint.
 * Scraping can take time; maxDuration is set in vercel.json (60s on Pro).
 */
module.exports = async (req, res) => {
  if (req.method !== 'POST') {
    return res.status(405).json({
      title: 'Method Not Allowed',
      detail: 'Use POST for /api/scrape',
    });
  }

  const orchestratorUrl = process.env.ORCHESTRATOR_URL;

  if (!orchestratorUrl) {
    return res.status(500).json({
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
      body: JSON.stringify(req.body ?? {}),
    });

    const body = await upstream.text();
    res.status(upstream.status);
    res.setHeader('Content-Type', upstream.headers.get('content-type') ?? 'application/json');
    return res.send(body);
  } catch (error) {
    return res.status(502).json({
      title: 'Upstream Error',
      detail: `Orchestrator scrape failed: ${error.message}`,
    });
  }
};
