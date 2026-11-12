@echo off
REM Executa o sistema. Uso: executar.bat [porta]   (porta padrao: 8080)
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo ERRO: Java nao encontrado. Instale o JDK 21 LTS em https://adoptium.net
  pause
  exit /b 1
)

if not exist marketplace-energia-solar.jar (
  echo Jar nao encontrado, compilando...
  call compilar.bat
  if errorlevel 1 exit /b 1
)

java -jar marketplace-energia-solar.jar %*
pause
