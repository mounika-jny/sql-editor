import React, { useState } from 'react';
import axios from 'axios';
import ResultsTable from './components/ResultsTable';
import QueryHistory from './components/QueryHistory';
import { API_ENDPOINTS } from './config';
import './App.css';

function App() {
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [queryHistory, setQueryHistory] = useState([]);

  const executeQuery = async () => {
    if (!query.trim()) {
      setError('Please enter a SQL query');
      return;
    }

    setLoading(true);
    setError(null);
    setResults(null);

    try {
      const response = await axios.post(API_ENDPOINTS.SQL_RUN, {
        query: query.trim(),
        page: page,
        size: size
      });

      const data = response.data;

      if (data.status === 'OK') {
        setResults(data.payload);

        // Add to history
        setQueryHistory(prev => [{
          query: query.trim(),
          timestamp: new Date().toLocaleString(),
          success: true,
          type: detectQueryType(query.trim())
        }, ...prev.slice(0, 9)]); // Keep last 10
      } else {
        setError(data.message || 'Query execution failed');
      }
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'An error occurred';
      setError(errorMsg);

      // Add failed query to history
      setQueryHistory(prev => [{
        query: query.trim(),
        timestamp: new Date().toLocaleString(),
        success: false,
        error: errorMsg
      }, ...prev.slice(0, 9)]);
    } finally {
      setLoading(false);
    }
  };

  const detectQueryType = (sql) => {
    const trimmed = sql.trim().toUpperCase();
    if (trimmed.startsWith('SELECT')) return 'SELECT';
    if (trimmed.startsWith('INSERT')) return 'INSERT';
    if (trimmed.startsWith('UPDATE')) return 'UPDATE';
    if (trimmed.startsWith('DELETE')) return 'DELETE';
    if (trimmed.startsWith('CREATE')) return 'CREATE';
    if (trimmed.startsWith('DROP')) return 'DROP';
    if (trimmed.startsWith('ALTER')) return 'ALTER';
    return 'OTHER';
  };

  const handleKeyPress = (e) => {
    if (e.ctrlKey && e.key === 'Enter') {
      executeQuery();
    }
  };

  const loadSampleQuery = () => {
    setQuery('SELECT 1 as id, \'John Doe\' as name, \'john@example.com\' as email UNION ALL\nSELECT 2, \'Jane Smith\', \'jane@example.com\'');
  };

  const clearResults = () => {
    setResults(null);
    setError(null);
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>🗄️ DS Editor</h1>
        <p>SQL Query Executor</p>
      </header>

      <div className="app-container">
        <div className="main-panel">
          <div className="query-panel">
            <div className="panel-header">
              <h2>Query Editor</h2>
              <div className="header-actions">
                <button onClick={loadSampleQuery} className="btn-secondary">
                  Load Sample
                </button>
                <button onClick={clearResults} className="btn-secondary">
                  Clear Results
                </button>
              </div>
            </div>

            <textarea
              className="query-input"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyPress}
              placeholder="Enter your SQL query here...&#10;&#10;Tip: Press Ctrl+Enter to execute"
              rows="8"
            />

            <div className="query-options">
              <div className="option-group">
                <label>
                  Page:
                  <input
                    type="number"
                    value={page}
                    onChange={(e) => setPage(Math.max(0, parseInt(e.target.value) || 0))}
                    min="0"
                  />
                </label>
                <label>
                  Size:
                  <input
                    type="number"
                    value={size}
                    onChange={(e) => setSize(Math.max(1, parseInt(e.target.value) || 20))}
                    min="1"
                  />
                </label>
              </div>

              <button 
                onClick={executeQuery} 
                disabled={loading}
                className="btn-primary"
              >
                {loading ? '⏳ Executing...' : '▶️ Execute Query'}
              </button>
            </div>
          </div>

          <div className="results-panel">
            <div className="panel-header">
              <h2>Results</h2>
            </div>

            {loading && (
              <div className="status-message loading">
                <div className="spinner"></div>
                <p>Executing query...</p>
              </div>
            )}

            {error && (
              <div className="status-message error">
                <strong>❌ Error:</strong>
                <p>{error}</p>
              </div>
            )}

            {!loading && !error && results && (
              <div className="results-content">
                {Array.isArray(results) ? (
                  <ResultsTable data={results} />
                ) : (
                  <div className="non-select-result">
                    <div className="success-icon">✅</div>
                    <h3>Query Executed Successfully</h3>
                    {results.updated !== undefined && (
                      <p className="result-detail">
                        <strong>Rows Affected:</strong> {results.updated}
                      </p>
                    )}
                    {results.message && (
                      <p className="result-detail">
                        <strong>Status:</strong> {results.message}
                      </p>
                    )}
                  </div>
                )}
              </div>
            )}

            {!loading && !error && !results && (
              <div className="status-message empty">
                <p>👆 Enter a query above and click "Execute Query" to see results</p>
              </div>
            )}
          </div>
        </div>

        <div className="side-panel">
          <QueryHistory history={queryHistory} onSelectQuery={setQuery} />
        </div>
      </div>
    </div>
  );
}

export default App;
