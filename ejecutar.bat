@echo off
rem Ejecuta el sistema de integrales (Windows).
rem   ejecutar.bat               interfaz grafica con SWI-Prolog
rem   ejecutar.bat --mock        interfaz grafica con respuestas simuladas
rem   ejecutar.bat "3*x^2" 0 2   modo consola
rem
rem Se ejecuta desde la carpeta del script para que encuentre config.properties y base_conocimiento\.

setlocal
cd /d "%~dp0"

set "JAR=integrales-declarativa-1.0.0.jar"
if not exist "%JAR%" set "JAR=target\integrales-declarativa-1.0.0.jar"
if not exist "%JAR%" (
    echo No se encontro el .jar. Compile primero con: mvnw.cmd clean package
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo No se encontro java. Instale el JDK 17 o superior.
    exit /b 1
)

java -jar "%JAR%" %*
exit /b %errorlevel%
