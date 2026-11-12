#!/bin/sh
# Executa o sistema. Uso: ./executar.sh [porta]   (porta padrao: 8080)
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "ERRO: Java nao encontrado. Instale o JDK 21 LTS em https://adoptium.net"
  exit 1
fi

if [ ! -f marketplace-energia-solar.jar ]; then
  echo "Jar nao encontrado, compilando..."
  sh ./compilar.sh || exit 1
fi

java -jar marketplace-energia-solar.jar "$@"
