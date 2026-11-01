#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 🏛️ SYNCHRONISATION ET SURVEILLANCE (NEXUSMEMORY v9.0) ==="
cd ~/nexus-core

# Pause de courtoisie pour laisser GitHub Actions instancier le nouveau run
echo "⏳ Synchronisation avec la forge cloud..."
sleep 6

# 1. Récupération dynamique du tout dernier run actif
echo "🔍 Recherche du run en cours..."
RUN_ID=$(gh run list --branch main --limit 1 --json databaseId,status --jq '.[0].databaseId')

if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
    echo "❌ Aucun run trouvé."
    exit 1
fi

echo "📌 Nouveau Run ID détecté : $RUN_ID"

# 2. Surveillance en direct de la compilation
echo "⏳ Attente active de la fin du build..."
gh run watch "$RUN_ID" --exit-status

# 3. Téléchargement sécurisé de l'artefact
echo "=== 📥 TÉLÉCHARGEMENT DE L'ARTEFACT ==="
rm -rf ./downloaded_artifact
mkdir -p ./downloaded_artifact
gh run download "$RUN_ID" --dir ./downloaded_artifact

# 4. Déplacement vers le stockage partagé
RAW_APK=$(find ./downloaded_artifact -name "*.apk" | head -n 1)
if [ -f "$RAW_APK" ]; then
    termux-setup-storage 2>/dev/null || true
    mkdir -p ~/storage/shared/Download
    
    TARGET_APK="$HOME/storage/shared/Download/NexusMemory-v9.0.apk"
    rm -f "$TARGET_APK"
    cp -f "$RAW_APK" "$TARGET_APK"
    
    echo "=== ✨ SUCCÈS ABSOLU : FICHIER PRÊT DANS LE DOSSIER DOWNLOAD ==="
    ls -lh "$TARGET_APK"
else
    echo "❌ Erreur : Aucun fichier APK trouvé dans l'artefact."
    exit 1
fi
