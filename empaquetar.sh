#!/bin/sh
# Genera el zip de entrega (Linux / macOS):  target/integrales-declarativa.zip
#
#   ./empaquetar.sh                 compila, corre las pruebas y empaqueta
#   ./empaquetar.sh -DskipTests     sin correr las pruebas
#
# El zip contiene lo que está COMMITEADO en HEAD (no los cambios locales ni archivos
# sin seguimiento) y el .jar ya compilado en la raíz, junto a ejecutar.sh y ejecutar.bat:
#
#   integrales-declarativa/
#     integrales-declarativa-1.0.0.jar
#     ejecutar.bat, ejecutar.sh, config.properties, README.md
#     base_conocimiento/, docs/, src/, pom.xml, mvnw, mvnw.cmd, .mvn/

set -e
cd "$(dirname "$0")"

NOMBRE=integrales-declarativa
JAR=target/$NOMBRE-1.0.0.jar
ZIP=target/$NOMBRE.zip

if ! git diff --quiet HEAD -- 2>/dev/null; then
    echo "Aviso: hay cambios sin commitear; el zip usa la versión commiteada (HEAD)." >&2
fi
for archivo in puente.pl bc_evaluacion.pl bc_integrales.pl bc_derivadas.pl \
               bc_simplificaciones_algebraicas.pl bc_simplificaciones.pl; do
    if ! git cat-file -e "HEAD:base_conocimiento/$archivo" 2>/dev/null; then
        echo "Aviso: falta base_conocimiento/$archivo en HEAD; el sistema no podrá usar Prolog." >&2
    fi
done

./mvnw -q -B clean package "$@"

git archive --format=zip --prefix="$NOMBRE/" --add-file="$JAR" -o "$ZIP" HEAD
echo "Listo: $ZIP"
