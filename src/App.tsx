import React, { useState, useEffect } from \"react\";
import { NexusMemoryService } from \"./services/NexusMemoryService\";

export default function App() {
  const [title, setTitle] = useState(\"\");
  const [content, setContent] = useState(\"\");
  const [query, setQuery] = useState(\"\");
  const [results, setResults] = useState<Array<{ title: string; content: string }>>([]);
  const [status, setStatus] = useState(\"\");

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !content) return;
    const success = await NexusMemoryService.save(title, content);
    if (success) {
      setStatus(\"Souvenir gravé avec succès dans le noyau FTS5.\");
      setTitle(\"\");
      setContent(\"\");
      executeSearch(\"\");
    } else {
      setStatus(\"Erreur lors de la gravure du souvenir.\");
    }
  };

  const executeSearch = async (searchTerm: string) => {
    const res = await NexusMemoryService.search(searchTerm);
    setResults(res);
  };

  useEffect(() => {
    executeSearch(\"\");
  }, []);

  return (
    <div style={{ padding: \"20px\", fontFamily: \"sans-serif\", background: \"#0f172a\", color: \"#f8fafc\", minHeight: \"100vh\", boxSizing: \"border-box\" }}>
      <h1 style={{ fontSize: \"24px\", fontWeight: \"bold\", marginBottom: \"5px\" }}>NexusMemory</h1>
      <p style={{ fontSize: \"14px\", color: \"#94a3b8\", marginBottom: \"20px\" }}>Noyau de persistance 24/7 & Recherche FTS5 ultrarapide</p>

      {/* Formulaire d ajout */}
      <form onSubmit={handleSave} style={{ background: \"#1e293b\", padding: \"15px\", borderRadius: \"8px\", marginBottom: \"20px\", border: \"1px solid #334155\" }}>
        <h2 style={{ fontSize: \"16px\", marginBottom: \"10px\", color: \"#38bdf8\" }}>Ajouter un Souvenir</h2>
        <input 
          type="text\" 
          placeholder=\"Titre du souvenir...\" 
          value={title} 
          onChange={e => setTitle(e.target.value)}
          style={{ width: \"100%\", padding: \"10px\", marginBottom: \"10px\", background: \"#0f172a\", border: \"1px solid #334155\", color: \"#fff\", borderRadius: \"4px\", boxSizing: \"border-box\" }}
        />
        <textarea 
          placeholder=\"Contenu mémoriel...\" 
          value={content} 
          onChange={e => setContent(e.target.value)}
          style={{ width: \"100%\", padding: \"10px\", marginBottom: \"10px\", background: \"#0f172a\", border: \"1px solid #334155\", color: \"#fff\", borderRadius: \"4px\", height: \"80px\", boxSizing: \"border-box\" }}
        />
        <button type="submit" style={{ background: \"#3b82f6\", color: \"#fff\", padding: \"10px 15px\", border: \"none\", borderRadius: \"4px\", fontWeight: \"bold\", width: \"100%\", cursor: \"pointer\" }}>Graver dans le Noyau</button>
        {status && <p style={{ fontSize: \"12px\", marginTop: \"10px\", color: \"#38bdf8\" }}>{status}</p>}
      </form>

      {/* Section de recherche */}
      <div style={{ background: \"#1e293b\", padding: \"15px\", borderRadius: \"8px\", border: \"1px solid #334155\" }}>
        <h2 style={{ fontSize: \"16px\", marginBottom: \"10px\", color: \"#38bdf8\" }}>Recherche FTS5 Foudroyante</h2>
        <input 
          type="text\" 
          placeholder=\"Rechercher un mot-clé...\" 
          value={query} 
          onChange={e => { 
            const val = e.target.value;
            setQuery(val); 
            executeSearch(val); 
          }}
          style={{ width: \"100%\", padding: \"10px\", marginBottom: \"15px\", background: \"#0f172a\", border: \"1px solid #334155\", color: \"#fff\", borderRadius: \"4px\", boxSizing: \"border-box\" }}
        />

        <div style={{ display: \"flex\", flexDirection: \"column\", gap: \"10px\" }}>
          {results.length === 0 ? (
            <p style={{ fontSize: \"14px\", color: \"#64748b\" }}>Aucun souvenir enregistré ou trouvé.</p>
          ) : (
            results.map((item, index) => (
              <div key={index} style={{ background: \"#0f172a\", padding: \"10px\", borderRadius: \"6px\", border: \"1px solid #334155\" }}>
                <h3 style={{ fontSize: \"15px\", fontWeight: \"bold\", color: \"#38bdf8\", marginBottom: \"4px\" }}>{item.title}</h3>
                <p style={{ fontSize: \"13px\", color: \"#cbd5e1\", margin: 0 }}>{item.content}</p>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
