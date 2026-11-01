#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 🏛️ RÉCUPÉRATION DE L'ARTEFACT OFFICIEL (NEXUSMEMORY v9.0) ==="
cd ~/nexus-core

# 1. Vérification de l'authentification GitHub CLI
if ! gh auth status &>/dev/null; then
    echo "⚠️ Veuillez vous authentifier à GitHub CLI :"
    gh auth login
fi

# 2. Récupération du dernier ID d'exécution (Run ID) réussi
echo "=== 🔍 RECHERCHE DU DERNIER BUILD RÉUSSI ==="
RUN_ID=$(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')
echo "ID du build : $RUN_ID"

# 3. Téléchargement de l'artefact
echo "=== 📥 TÉLÉCHARGEMENT DE L'APK ==="
gh run download "$RUN_ID" --dir ./downloaded_artifact

# 4. Copie vers le dossier Download partagé
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
    echo "❌ L'artefact APK n'est pas encore prêt ou le build est en cours sur GitHub."
    exit 1
fi
