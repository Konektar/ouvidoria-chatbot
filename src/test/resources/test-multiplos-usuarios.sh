#!/bin/bash

BASE_URL="https://5d02c85e0ae0.ngrok-free.app/ouvidoria/webhook"

echo "👥 TESTE DE MÚLTIPLOS USUÁRIOS SIMULTÂNEOS"
echo "==========================================="

# Função para testar um usuário específico
test_user() {
    local user_id=$1
    local phone="55119999999$user_id"

    echo "👤 Usuário $user_id ($phone) iniciando teste..."

    # Apenas envia início para testar concorrência
    curl -s -X POST "$BASE_URL/zapi" \
        -H "Content-Type: application/json" \
        -d "{\"message\": {\"from\": \"$phone\", \"text\": \"inicio\"}}" \
        -w " | Status: %{http_code}\n"
}

# Executar testes para 3 usuários
for i in {01..03}; do
    test_user $i &
done

# Aguardar todos os processos
wait
echo "🎉 Teste de múltiplos usuários concluído!"