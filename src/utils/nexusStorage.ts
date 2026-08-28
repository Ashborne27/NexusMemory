// Pont de synchronisation et de persistance infaillible NexusMemory

export const NexusStorage = {
  save(key: string, data: any): void {
    const serialized = JSON.stringify(data);
    try {
      if ((window as any).AndroidNexus) {
        (window as any).AndroidNexus.saveData(key, serialized);
      } else {
        localStorage.setItem(key, serialized);
      }
    } catch (e) {
      console.error("Erreur de sauvegarde persistante:", e);
    }
  },

  load(key: string): any {
    try {
      let raw = null;
      if ((window as any).AndroidNexus) {
        raw = (window as any).AndroidNexus.getData(key);
      } else {
        raw = localStorage.getItem(key);
      }
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      console.error("Erreur de chargement persistant:", e);
      return null;
    }
  },

  log(message: string): void {
    if ((window as any).AndroidNexus) {
      (window as any).AndroidNexus.logToNative(message);
    } else {
      console.log("[NexusWeb]:", message);
    }
  }
};
