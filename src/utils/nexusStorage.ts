// Pont de synchronisation et de persistance ultra-fluide avec Debounce

let saveTimeouts: { [key: string]: any } = {};

export const NexusStorage = {
  save(key: string, data: any, delay: number = 300): void {
    const serialized = JSON.stringify(data);
    
    // Annulation du timeout précédent pour éviter la saturation du pont lors de la frappe rapide
    if (saveTimeouts[key]) {
      clearTimeout(saveTimeouts[key]);
    }

    // Sauvegarde différée (Debounce) pour garantir zéro latence à la saisie
    saveTimeouts[key] = setTimeout(() => {
      try {
        if ((window as any).AndroidNexus) {
          (window as any).AndroidNexus.saveData(key, serialized);
        } else {
          localStorage.setItem(key, serialized);
        }
      } catch (e) {
        console.error("Erreur de sauvegarde persistante:", e);
      }
    }, delay);
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
