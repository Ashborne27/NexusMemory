import { registerPlugin } from \"@capacitor/core\";

export interface NexusMemoryPluginInterface {
  saveMemory(options: { title: string; content: string }): Promise<{ success: boolean; id: number }>;
  searchMemories(options: { query: string }): Promise<{ results: Array<{ title: string; content: string }> }>;
}

const NexusMemory = registerPlugin<NexusMemoryPluginInterface>("NexusMemoryPlugin");

export class NexusMemoryService {
  /**
   * Enregistre un souvenir dans la base de données FTS5 locale de manière instantanée
   */
  static async save(title: string, content: string): Promise<boolean> {
    try {
      const res = await NexusMemory.saveMemory({ title, content });
      return res.success;
    } catch (e) {
      console.error("Erreur lors de la sauvegarde du souvenir :", e);
      return false;
    }
  }

  /**
   * Effectue une recherche textuelle foudroyante via FTS5
   */
  static async search(query: string): Promise<Array<{ title: string; content: string }>> {
    try {
      const res = await NexusMemory.searchMemories({ query });
      return res.results || [];
    } catch (e) {
      console.error("Erreur lors de la recherche :", e);
      return [];
    }
  }
}
