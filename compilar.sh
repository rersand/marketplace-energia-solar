#!/bin/sh
# Compila o projeto e gera o jar executavel marketplace-energia-solar.jar
set -e
cd "$(dirname "$0")"

if ! command -v javac >/dev/null 2>&1; then
  echo "ERRO: javac nao encontrado. Instale o JDK 21 LTS em https://adoptium.net"
  exit 1
fi

rm -rf bin && mkdir bin
find src -name "*.java" > bin/fontes.txt
javac --release 17 -encoding UTF-8 -cp "lib/sqlite-jdbc-3.46.1.3.jar" -d bin @bin/fontes.txt
javac --release 8 -nowarn -encoding UTF-8 -d bin src/com/energiasolar/Inicializador.java 2>/dev/null
rm bin/fontes.txt

printf 'Main-Class: com.energiasolar.Inicializador\nClass-Path: lib/sqlite-jdbc-3.46.1.3.jar\n' > bin/MANIFEST.MF
jar cfm marketplace-energia-solar.jar bin/MANIFEST.MF -C bin com
rm bin/MANIFEST.MF

echo "OK: marketplace-energia-solar.jar gerado."
