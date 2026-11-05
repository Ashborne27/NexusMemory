#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 🏛️ SYNCHRONISATION ET SURVEILLANCE (NEXUS MEMORY v11.0) ==="
cd ~/nexus-core

# Pause de courtoisie pour laisser GitHub Actions initialiser le webhook
echo "⏳ Synchronisation avec la forge cloud..."
sleep 6

# 1. Récupération dynamique du tout dernier Run ID sur la branche main
echo "🔭 Recherche du run en cours..."
RUN_ID=$(gh gh run list --branch main --limit 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null || gh run list --branch main --limit 1 --json databaseId --jq '.[0].databaseId')

if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
    RUN_ID=$(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')
fi

if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
    echo "❌ Aucun run trouvé."
    exit 1
fi

echo "📌 Nouveau Run ID détecté : $RUN_ID"

# 2. Surveillance en direct de la compilation
echo "⏳ Attente active de la fin du build..."
gh run watch "$RUN_ID" --exit-status || true

# 3. Téléchargement sécurisé de l'artefact (gère tous les noms d'artefacts)
echo "=== 📥 TÉLÉCHARGEMENT DE L'ARTEFACT ==="
rm -rf ./downloaded_artifact
mkdir -p ./downloaded_artifact

# Téléchargement global de tous les artefacts du run sans restriction de nom
gh run download "$RUN_ID" --dir ./downloaded_artifact || {
    echo "⚠️ Téléchargement direct échoué, tentative via l'API GitHub..."
    # Fallback de sécurité
    DOWNLOAD_URL=$(gh api "repos/{owner}/{repo}/actions/runs/$RUN_ID/artifacts" --jq '.artifacts[0].archive_download_url')
    echo "URL trouvée, mais utilisation du téléchargement standard recommandée."
}

# 4. Déplacement vers le stockage partagé (Dossier Download)
RAW_APK=$(find ./downloaded_artifact -name "*.apk" | head -n 1)

if [ -n "$RAW_APK" ] && [ -f "$RAW_APK" ]; then
    termux-setup-storage 2>/dev/null || true
    mkdir -p /storage/emulated/0/Download
    cp -f "$RAW_APK" /storage/emulated/0/Download/NexusMemory-v11.0.apk
    echo "=== 🌟 SUCCÈS ABSOLU : FICHIER PRÊT DANS LE DOSSIER DOWNLOAD ==="
    ls -lh /storage/emulated/0/Download/NexusMemory-v11.0.apk
else
    echo "❌ Erreur : Aucun fichier .apk trouvé dans l'artefact téléchargé."
    exit 1
fi

