#!/bin/bash

BASE_URL="https://5d02c85e0ae0.ngrok-free.app/ouvidoria/webhook"

echo "🐛 TESTE DE ERROS E CASOS LIMITE"
echo "================================"

test_error_case() {
    local description=$1
    local payload=$2

    echo "🧪 Testando: $description"
    response=$(curl -s -X POST "$BASE_URL/zapi" \
        -H "Content-Type: application/json" \
        -d "$payload" \
        -w " | Status: %{http_code}")

    echo "📋 Resultado: $response"
    echo "---"
}

# Casos de teste para erros
test_error_case "Payload vazio" "{}"
test_error_case "Message vazia" '{"message": {}}'
test_error_case "Sem campo from" '{"message": {"text": "teste"}}'
test_error_case "Sem campo text" '{"message": {"from": "5511999999999"}}'
test_error_case "Campo text vazio" '{"message": {"from": "5511999999999", "text": ""}}'