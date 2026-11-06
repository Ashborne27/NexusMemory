#!/bin/bash
echo "=== 🏛️ PROTOCOLE DE SURVEILLANCE ET RÉCUPÉRATION v12.0 ==="

# Récupération du SHA du commit actuel (HEAD)
HEAD_SHA=$(git rev-parse HEAD)
echo "🎯 Commit SHA ciblé : $HEAD_SHA"

RUN_ID=""
while [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; do
    echo "⏳ En attente du run GitHub Actions pour le commit $HEAD_SHA..."
    RUN_ID=$(gh run list --branch feature/v12-core --commit "$HEAD_SHA" --limit 1 --json databaseId --jq '.[0].databaseId')
    if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
        sleep 5
    fi
done

echo "🚀 Run ID v12.0 détecté avec succès : $RUN_ID"
echo "Surveillance de la compilation en cours sur la forge..."

# Surveillance active du run
gh run watch $RUN_ID --exit-status || true

echo "=== 📥 TÉLÉCHARGEMENT DE L'ARTEFACT v12.0 ==="
rm -rf ~/nexus_artifact && mkdir -p ~/nexus_artifact
gh run download $RUN_ID --dir ~/nexus_artifact

# Transfert et renommage vers le dossier Download de l'appareil
mkdir -p ~/storage/emulated/0/Download
TARGET_APK="/storage/emulated/0/Download/NexusMemory-v12.0.apk"
FOUND_APK=$(find ~/nexus_artifact -name "*.apk" | head -n 1)

if [ -n "$FOUND_APK" ]; then
    cp "$FOUND_APK" "$TARGET_APK"
    echo "=== 🌟 SUCCÈS ABSOLU : NEXUSMEMORY v12.0 PRÊT DANS DOWNLOAD ==="
    ls -lh "$TARGET_APK"
else
    echo "❌ Erreur critique : Aucun fichier .apk détecté pour la v12.0."
    exit 1
fi
