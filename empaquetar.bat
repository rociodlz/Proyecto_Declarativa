@echo off
rem Genera el zip de entrega (Windows):  target\integrales-declarativa.zip
rem
rem   empaquetar.bat                 compila, corre las pruebas y empaqueta
rem   empaquetar.bat -DskipTests     sin correr las pruebas
rem
rem El zip contiene lo que esta COMMITEADO en HEAD (no los cambios locales ni archivos
rem sin seguimiento) y el .jar ya compilado en la raiz, junto a ejecutar.sh y ejecutar.bat.
rem Requiere git en el PATH.

setlocal
cd /d "%~dp0"

set "NOMBRE=integrales-declarativa"
set "JAR=target/%NOMBRE%-1.0.0.jar"
set "ZIP=target/%NOMBRE%.zip"

git diff --quiet HEAD --
if errorlevel 1 echo Aviso: hay cambios sin commitear; el zip usa la version commiteada (HEAD).
for %%A in (puente.pl bc_evaluacion.pl bc_integrales.pl bc_derivadas.pl bc_simplificaciones_algebraicas.pl bc_simplificaciones.pl) do (
    git cat-file -e "HEAD:base_conocimiento/%%A" 2>nul
    if errorlevel 1 echo Aviso: falta base_conocimiento/%%A en HEAD; el sistema no podra usar Prolog.
)

call mvnw.cmd -q -B clean package %*
if errorlevel 1 exit /b 1

git archive --format=zip --prefix=%NOMBRE%/ --add-file=%JAR% -o %ZIP% HEAD
if errorlevel 1 exit /b 1
echo Listo: %ZIP%
