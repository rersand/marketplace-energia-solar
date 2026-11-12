@echo off
REM Compila o projeto e gera o jar executavel marketplace-energia-solar.jar
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
  echo ERRO: javac nao encontrado. Instale o JDK 21 LTS em https://adoptium.net
  pause
  exit /b 1
)

if exist bin rmdir /s /q bin
mkdir bin
setlocal enabledelayedexpansion
set "RAIZ=%CD%\"
(for /r src %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:%RAIZ%=!"
  echo !p:\=/!
)) > bin\fontes.txt
javac --release 17 -encoding UTF-8 -cp "lib\sqlite-jdbc-3.46.1.3.jar" -d bin @bin\fontes.txt
if errorlevel 1 ( pause & exit /b 1 )
javac --release 8 -nowarn -encoding UTF-8 -d bin src\com\energiasolar\Inicializador.java 2>nul
del bin\fontes.txt

(echo Main-Class: com.energiasolar.Inicializador& echo Class-Path: lib/sqlite-jdbc-3.46.1.3.jar)> bin\MANIFEST.MF
jar cfm marketplace-energia-solar.jar bin\MANIFEST.MF -C bin com
del bin\MANIFEST.MF

echo OK: marketplace-energia-solar.jar gerado.
