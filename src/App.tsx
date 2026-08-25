import React, { useState, useEffect } from 'react';

export default function App() {
  const [metrics, setMetrics] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const timer = setInterval(() => {
      setMetrics({
        daemonStatus: 'ACTIVE',
        ramUsage: '42%',
        uptime: '99.9%',
        timestamp: new Date().toLocaleTimeString()
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  return (
    <div style={{ padding: '20px', fontFamily: 'monospace', backgroundColor: '#0f172a', color: '#38bdf8', minHeight: '100vh' }}>
      <h1 style={{ borderBottom: '2px solid #38bdf8', paddingBottom: '10px' }}>NexusMemory Telemetry</h1>
      {error && <div style={{ color: '#f87171', marginBottom: '10px' }}>Error: {error}</div>}
      {metrics ? (
        <div style={{ marginTop: '20px', background: '#1e293b', padding: '15px', borderRadius: '8px' }}>
          <p><strong>Daemon Status:</strong> <span style={{ color: '#4ade80' }}>{metrics.daemonStatus}</span></p>
          <p><strong>RAM Usage:</strong> {metrics.ramUsage}</p>
          <p><strong>System Uptime:</strong> {metrics.uptime}</p>
          <p><strong>Last Sync:</strong> {metrics.timestamp}</p>
        </div>
      ) : (
        <p>Initializing telemetry stream...</p>
      )}
    </div>
  );
}
