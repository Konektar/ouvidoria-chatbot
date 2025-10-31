#!/bin/bash

BASE_URL="https://5d02c85e0ae0.ngrok-free.app/ouvidoria/webhook"
PHONE="5511888888888"

echo "🎭 TESTANDO FLUXO ANÔNIMO COM DENÚNCIA"
echo "========================================"

make_request() {
    local step=$1
    local message=$2
    echo "📝 Passo $step: Enviando '$message'"

    response=$(curl -s -X POST "$BASE_URL/zapi" \
        -H "Content-Type: application/json" \
        -d "{\"message\": {\"from\": \"$PHONE\", \"text\": \"$message\"}}" \
        -w " | HTTP Status: %{http_code}")

    echo "✅ Resposta: $response"
    echo "---"
    sleep 2
}

make_request "1" "inicio"
make_request "2" "Anonimato"
make_request "3" "Concordo"
make_request "4" "DENUNCIA"
make_request "5" "ASSEDIO"
make_request "6" "Presenciei situações de assédio moral no setor de produção. O supervisor faz comentários constrangedores e ameaça os funcionários."
make_request "7" "Confirmar"