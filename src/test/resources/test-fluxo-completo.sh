#!/bin/bash

BASE_URL="https://5d02c85e0ae0.ngrok-free.app/ouvidoria/webhook"
PHONE="5511999999999"

echo "🚀 INICIANDO TESTES COMPLETOS DO CHATBOT"
echo "=========================================="

# Verificar saúde do sistema primeiro
echo "🔍 Verificando saúde do sistema..."
curl -X GET "$BASE_URL/health"
echo -e "\n---"

# Teste simples
echo "🧪 Teste básico..."
curl -X GET "$BASE_URL/test"
echo -e "\n---"

# Função para fazer requisições ao webhook
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

# Fluxo de teste completo
echo "🎯 Testando fluxo COMPLETO de manifestação..."
make_request "1" "inicio"
make_request "2" "Sim"
make_request "3" "João Silva, 5511999999999, joao.silva@email.com"
make_request "4" "Concordo"
make_request "5" "ELOGIO"
make_request "6" "Quero elogiar o atendimento excelente que recebi da equipe de suporte. Foram muito prestativos e resolveram meu problema rapidamente."
make_request "7" "Confirmar"

echo "✅ Teste de fluxo completo concluído!"