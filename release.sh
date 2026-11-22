#!/usr/bin/env bash
# Publica una versión: compila el APK, calcula su SHA-256 y actualiza update/version.json.
# Uso:  ./release.sh <versionCode> <versionName> "notas de la versión"
# Ej:   ./release.sh 2 1.1 "Arreglado el modo Sport"
set -euo pipefail
cd "$(dirname "$0")"

CODE="${1:?falta versionCode (ej 2)}"
NAME="${2:?falta versionName (ej 1.1)}"
NOTES="${3:-Actualización $NAME}"
USER_REPO="${REPO:-LuisDavidFajardoH/ex2}"   # export REPO=tuusuario/ex2-lite

# 1. Sube versionCode/versionName en el manifest y compila
sed -i -E "s/(android:versionCode=\")[0-9]+(\")/\1$CODE\2/; s/(android:versionName=\")[^\"]+(\")/\1$NAME\2/" app/AndroidManifest.xml
( cd app && ./build.sh )

# 2. Copia el APK al repo (update/) y calcula su SHA-256
cp app/ex2lite.apk update/ex2lite.apk
SHA=$(sha256sum update/ex2lite.apk | cut -d' ' -f1)

# 3. Escribe el manifest de actualización
cat > update/version.json <<EOF
{
  "versionCode": $CODE,
  "versionName": "$NAME",
  "apkUrl": "https://raw.githubusercontent.com/$USER_REPO/main/update/ex2lite.apk",
  "sha256": "$SHA",
  "notes": "$NOTES"
}
EOF

echo "Listo. version.json:"
cat update/version.json
echo
echo "Ahora sube el cambio:"
echo "  git add -A && git commit -m \"v$NAME\" && git push"
