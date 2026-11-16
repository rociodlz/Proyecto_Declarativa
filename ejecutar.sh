#!/bin/sh
# Ejecuta el sistema de integrales (Linux / macOS).
#   ./ejecutar.sh               interfaz gráfica con SWI-Prolog
#   ./ejecutar.sh --mock        interfaz gráfica con respuestas simuladas
#   ./ejecutar.sh "3*x^2" 0 2   modo consola
#
# Se ejecuta desde la carpeta del script para que encuentre config.properties y base_conocimiento/.

cd "$(dirname "$0")" || exit 1

JAR=integrales-declarativa-1.0.0.jar
[ -f "$JAR" ] || JAR="target/$JAR"
if [ ! -f "$JAR" ]; then
    echo "No se encontró el .jar. Compile primero con: ./mvnw clean package" >&2
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo "No se encontró java. Instale el JDK 17 o superior." >&2
    exit 1
fi

exec java -jar "$JAR" "$@"
