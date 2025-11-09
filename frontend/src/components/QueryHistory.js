import React from 'react';
import './QueryHistory.css';

function QueryHistory({ history, onSelectQuery }) {
  if (history.length === 0) {
    return (
      <div className="query-history">
        <div className="panel-header">
          <h3>📜 Query History</h3>
        </div>
        <div className="history-empty">
          <p>No queries executed yet</p>
        </div>
      </div>
    );
  }

  return (
    <div className="query-history">
      <div className="panel-header">
        <h3>📜 Query History</h3>
        <span className="history-count">{history.length}</span>
      </div>

      <div className="history-list">
        {history.map((item, index) => (
          <div 
            key={index} 
            className={`history-item ${item.success ? 'success' : 'error'}`}
            onClick={() => onSelectQuery(item.query)}
          >
            <div className="history-header">
              <span className={`query-type ${item.type?.toLowerCase()}`}>
                {item.type || 'SQL'}
              </span>
              <span className="history-time">{item.timestamp}</span>
            </div>

            <div className="history-query">
              {item.query.length > 60 
                ? item.query.substring(0, 60) + '...' 
                : item.query}
            </div>

            {!item.success && item.error && (
              <div className="history-error">
                ❌ {item.error}
              </div>
            )}

            {item.success && (
              <div className="history-success">
                ✅ Success
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

export default QueryHistory;
