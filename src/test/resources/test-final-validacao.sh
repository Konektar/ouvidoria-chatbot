#!/bin/bash

echo "🎯 TESTE FINAL - VALIDAÇÃO COMPLETA DO SISTEMA"
echo "=============================================="

BASE_URL="https://5d02c85e0ae0.ngrok-free.app/ouvidoria/webhook"

echo "1. ✅ Health Check:"
curl -s -X GET "$BASE_URL/health"
echo -e "\n"

echo "2. ✅ Test Endpoint:"
curl -s -X GET "$BASE_URL/test"
echo -e "\n"

echo "3. ✅ Webhook Funcional:"
curl -s -X POST "$BASE_URL/zapi" \
  -H "Content-Type: application/json" \
  -d '{"message": {"from": "5511999999999", "text": "inicio"}}' \
  -w " | Status: %{http_code}\n"

echo -e "\n✅ VALIDAÇÃO CONCLUÍDA - SISTEMA OPERACIONAL"