import { useEffect, useState } from 'react';
import { registerPlugin } from '@capacitor/core';

const NexusTelemetry = registerPlugin<{ getSystemMetrics(): Promise<any> }>('NexusTelemetry');

export default function App() {
  const [metrics, setMetrics] = useState<any>(null);
  const [error, setError] = useState<string | '\>( '\ );

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const res = await NexusTelemetry.getSystemMetrics();
        setMetrics(res);
      } catch (err: any) {
        setError(err.message || 'Erreur de communication');
      }
    };

    fetchMetrics();
    const interval = setInterval(fetchMetrics, 2000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div style={{ padding: '20px', background: '#0f172a', color: '#f8fafc', minHeight: '100vh', fontFamily: 'monospace' }}>
      <div style={{ textAlign: 'center', marginBottom: '20px' }}>
        <h1 style={{ color: '#38bdf8', fontSize: '22px', margin: '0 0 5px 0' }}>NexusMemory</h1>
        <p style={{ color: '#94a3b8', fontSize: '12px', margin: 0 }}>Noyau de virtualisation et persistence souveraine actif.</p>
      </div>

      <div style={{ background: '#1e293b', padding: '15px', borderRadius: '12px', border: '1px solid #38bdf8', boxShadow: '0 0 20px rgba(56, 189, 248, 0.2)' }}>
        <h3 style={{ margin: '0 0 12px 0', color: '#f43f5e', fontSize: '15px' }}>⚡ TÉLÉMÉTRIE SOUVERAINE</h3>
        {error && <p style={{ color: '#ef4444', fontSize: '12px' }}>Erreur : {error}</p>}
        {metrics ? (
          <div style={{ fontSize: '13px', lineHeight: '1.8' }}>
            <div>🛡️ Daemon : <span style={{ color: '#10b981', fontWeight: 'bold' }}>{metrics.daemonStatus}</span></div>
            <div>🧠 RAM Utilisée : <b style={{ color: '#38bdf8' }}>{metrics.usedMemoryMB} MB</b> / {metrics.maxMemoryMB} MB</div>
            <div>🔋 RAM Libre : <b style={{ color: '#38bdf8' }}>{metrics.freeMemoryMB} MB</b></div>
            <div>⚡ Transactions Vault : <b style={{ color: '#fbbf24' }}>{metrics.vaultTransactions}</b></div>
          </div>
        ) : (
          <p style={{ color: '#94a3b8', fontSize: '13px' }}>Connexion au noyau en cours...</p>
        )}
      </div>

      <div style={{ marginTop: '20px', background: '#1e293b', padding: '12px' , borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '10px' }}>
        <div style={{ width: '10px', height: '10px', borderRadius: '50%' , background: '#10b981', boxShadow: '0 0 8px #10b981' }}></div>
        <span style={{ fontSize: '13px', color: '#10b981', fontWeight: 'bold' }}>Service Foreground Actif</span>
      </div>
    </div>
  );
}
