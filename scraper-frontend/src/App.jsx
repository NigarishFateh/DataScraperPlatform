import { useEffect, useState } from 'react';
import { checkHealth, runScrape } from './api/scraperApi';
import './App.css';

const SOURCES = [
  { id: 'google', label: 'Google' },
  { id: 'microsoft', label: 'Microsoft' },
  { id: 'ibm', label: 'IBM' },
];

const CATEGORIES = [
  { id: 'jobs', label: 'Jobs' },
  { id: 'products', label: 'Products' },
  { id: 'services', label: 'Services' },
  { id: 'company_info', label: 'Company Info' },
  { id: 'contacts', label: 'Contacts' },
  { id: 'news', label: 'News' },
];

function App() {
  const [selectedSources, setSelectedSources] = useState(['google', 'microsoft', 'ibm']);
  const [selectedCategories, setSelectedCategories] = useState(['jobs']);
  const [health, setHealth] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkHealth()
      .then(setHealth)
      .catch(() =>
        setHealth({
          status: 'DOWN',
          message: 'Start backend services with .\\start-all-services.ps1',
        }),
      );
  }, []);

  function toggleValue(list, setList, value) {
    setList((current) =>
      current.includes(value)
        ? current.filter((item) => item !== value)
        : [...current, value],
    );
  }

  async function handleScrape() {
    setError('');
    setResult(null);

    if (selectedSources.length === 0 || selectedCategories.length === 0) {
      setError('Select at least one source and one category.');
      return;
    }

    setLoading(true);
    try {
      const response = await runScrape({
        sources: selectedSources,
        categories: selectedCategories,
      });
      setResult(response);
    } catch (scrapeError) {
      setError(scrapeError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <header className="hero">
        <div>
          <p className="eyebrow">Data Scraper Platform</p>
          <h1>Scrape IT company websites</h1>
          <p className="subtitle">
            Choose sources and categories, then let the orchestrator call the scraper microservices in parallel.
          </p>
        </div>
        <div className={`status-pill ${health?.status === 'UP' ? 'up' : 'down'}`}>
          Orchestrator: {health?.status ?? 'CHECKING'}
          {health?.status === 'DOWN' && (
            <span className="status-note"> — run start-all-services.ps1</span>
          )}
        </div>
      </header>

      <main className="layout">
        <section className="panel">
          <h2>Scrape configuration</h2>

          <div className="field-group">
            <h3>Sources</h3>
            <div className="chip-grid">
              {SOURCES.map((source) => (
                <label key={source.id} className="chip">
                  <input
                    type="checkbox"
                    checked={selectedSources.includes(source.id)}
                    onChange={() => toggleValue(selectedSources, setSelectedSources, source.id)}
                  />
                  <span>{source.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="field-group">
            <h3>Categories</h3>
            <div className="chip-grid">
              {CATEGORIES.map((category) => (
                <label key={category.id} className="chip">
                  <input
                    type="checkbox"
                    checked={selectedCategories.includes(category.id)}
                    onChange={() => toggleValue(selectedCategories, setSelectedCategories, category.id)}
                  />
                  <span>{category.label}</span>
                </label>
              ))}
            </div>
          </div>

          <button className="primary-button" onClick={handleScrape} disabled={loading}>
            {loading ? 'Scraping...' : 'Start Scrape'}
          </button>

          {error && <p className="error">{error}</p>}
        </section>

        <section className="panel results-panel">
          <h2>Results</h2>

          {!result && !loading && (
            <p className="placeholder">Run a scrape to see results here.</p>
          )}

          {loading && <p className="placeholder">Calling orchestrator and scraper services...</p>}

          {result && (
            <div className="summary">
              <div>
                <span className="label">Status</span>
                <strong>{result.status}</strong>
              </div>
              <div>
                <span className="label">Elapsed</span>
                <strong>{result.elapsedMs} ms</strong>
              </div>
              <div>
                <span className="label">Result sets</span>
                <strong>{result.results?.length ?? 0}</strong>
              </div>
            </div>
          )}

          {result?.results?.map((scrapeResult) => (
            <article key={`${scrapeResult.source}-${scrapeResult.category}`} className="result-card">
              <div className="result-header">
                <h3>
                  {scrapeResult.source} · {scrapeResult.category}
                </h3>
                <span className="badge">{scrapeResult.totalItems} items</span>
              </div>
              <p className="meta">{scrapeResult.pageTitle}</p>

              {scrapeResult.metadata?.status && scrapeResult.metadata.status !== 'SUCCESS' && (
                <p className="warning">
                  Status: {scrapeResult.metadata.status}
                  {scrapeResult.metadata.error ? ` — ${scrapeResult.metadata.error}` : ''}
                </p>
              )}

              {scrapeResult.items?.length > 0 ? (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Title</th>
                        <th>Location</th>
                        <th>URL</th>
                      </tr>
                    </thead>
                    <tbody>
                      {scrapeResult.items.map((item, index) => (
                        <tr key={`${item.title}-${index}`}>
                          <td>{item.title}</td>
                          <td>{item.location || '—'}</td>
                          <td>
                            {item.url ? (
                              <a href={item.url} target="_blank" rel="noreferrer">
                                Open
                              </a>
                            ) : (
                              '—'
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="placeholder">No items returned for this source/category.</p>
              )}
            </article>
          ))}
        </section>
      </main>
    </div>
  );
}

export default App;
