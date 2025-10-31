Perfeito! Com todas as informações recebidas, vou criar um README.md completo para seu projeto de Chatbot de Ouvidoria.

Aqui está a documentação completa:

```markdown
# 🤖 Chatbot de Ouvidoria - Z-API + Spring Boot

Um sistema de atendimento automatizado para ouvidoria via WhatsApp, desenvolvido em Spring Boot 3.2 com integração ao Z-API e armazenamento em Google Sheets.

## 📋 Funcionalidades

- **Atendimento via WhatsApp** integrado com Z-API
- **Fluxo conversacional** guiado para diferentes tipos de manifestação
- **Suporte a usuários anônimos** e identificados
- **Consentimento LGPD** integrado no fluxo
- **Armazenamento local** (H2) e **Google Sheets**
- **Geração automática de protocolos**
- **Menu interativo** com botões

## 🏗️ Arquitetura do Sistema

```
┌─────────────┐    ┌─────────────┐    ┌──────────────┐
│   Z-API     │───▶│  Spring Boot│───▶│  Database    │
│  WhatsApp   │    │   Controller│    │  (H2)        │
└─────────────┘    └─────────────┘    └──────────────┘
│
▼
┌──────────────┐
│ Google Sheets│
│  (Relatório) │
└──────────────┘
```

## 📦 Tecnologias Utilizadas

- **Java 17** + **Spring Boot 3.2.0**
- **Spring Data JPA** + **Hibernate**
- **H2 Database** (dev) / **PostgreSQL** (prod)
- **Google Sheets API**
- **Z-API** para integração com WhatsApp
- **Maven** para build

## 🚀 Como Executar a Aplicação

### Pré-requisitos

- Java 17 ou superior
- Maven 3.6+
- Conta no Z-API
- Conta Google Cloud (para Sheets API)

### 1. Clone e configure o projeto

```bash
git clone <seu-repositorio>
cd ouvidoria-chatbot
```

### 2. Configure o arquivo `application.properties`

```properties
# Servidor
server.port=8080

# Banco de dados (H2 para desenvolvimento)
spring.datasource.url=jdbc:h2:file:./data/ouvidoria;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Z-API Configuration
zapi.api.token=SEU_TOKEN_ZAPI
zapi.base.url=https://api.z-api.io
zapi.instance.id=SUA_INSTANCE_ID
zapi.client.token=SEU_CLIENT_TOKEN

# Google Sheets
app.sheets.manifestacoes.spreadsheet-id=ID_DA_SUA_PLANILHA
app.sheets.manifestacoes.sheet-name=Manifestacoes

# Sanity Check
app.sanity-check.enabled=true
```

### 3. Configuração do Google Sheets API

#### Passo 1: Acesse o Google Cloud Console
- Vá para [Google Cloud Console](https://console.cloud.google.com)
- Crie um novo projeto ou selecione um existente

#### Passo 2: Ative a Google Sheets API
- No menu lateral, clique em **"APIs & Services"** > **"Library"**
- Pesquise por **"Google Sheets API"**
- Clique em **"Enable"**

#### Passo 3: Crie uma Service Account
- Vá para **"APIs & Services"** > **"Credentials"**
- Clique em **"Create Credentials"** > **"Service Account"**
- Preencha:
    - **Service account name**: `ouvidoria-chatbot`
    - **Service account ID**: gerado automaticamente
    - **Description**: `Service account para chatbot de ouvidoria`

#### Passo 4: Baixe o arquivo de credenciais
- Na lista de service accounts, clique no email da conta criada
- Vá para a aba **"Keys"**
- Clique em **"Add Key"** > **"Create new key"**
- Selecione **"JSON"** e clique em **"Create"**
- O arquivo JSON será baixado automaticamente

#### Passo 5: Configure as credenciais no projeto
- Renomeie o arquivo baixado para `credentials.json`
- Coloque na pasta `src/main/resources/`

```
src/
└── main/
    └── resources/
        ├── application.properties
        └── credentials.json  ← Coloque aqui!
```

#### Passo 6: Compartilhe a planilha
- Crie uma planilha no Google Sheets
- Compartilhe com o email da service account (encontrado no campo `client_email` do credentials.json)
- Dê permissão de **"Editor"**

### 4. Build e execução

```bash
# Build do projeto
mvn clean package

# Executar
java -jar target/ouvidoria-chatbot-1.0.0.jar

# Ou executar com Maven
mvn spring-boot:run
```

### 5. Verifique se está funcionando

Acesse: `http://localhost:8080/webhook/health`

Resposta esperada:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00",
  "service": "Ouvidoria Chatbot"
}
```

## 🔧 Configuração do Z-API

### 1. Crie uma conta no Z-API
- Acesse [Z-API](https://z-api.io)
- Crie uma conta e faça login

### 2. Crie uma instância
- No painel, clique em **"Criar Instância"**
- Escolha o tipo **"Profissional"** ou **"Negócios"**
- Siga as instruções para vincular seu WhatsApp

### 3. Obtenha os tokens
- **Instance ID**: Encontrado na lista de instâncias
- **API Token**: Clique na instância > **"Token"** > copie o token
- **Client Token**: Disponível nas configurações da instância

### 4. Configure o webhook
- Na sua instância, vá em **"Webhook"**
- Configure a URL: `http://seu-servidor:8080/webhook/zapi`
- Marque as opções:
    - ✅ Mensagens
    - ✅ Status das Mensagens

## 📱 Fluxo do Chatbot

```
┌─────────────────┐
│    INÍCIO       │ ← Bem-vindo + identificação
└─────────────────┘
         │
         ▼
┌─────────────────┐
│ IDENTIFICAÇÃO   │ ← Anônimo ou Identificado
└─────────────────┘
         │
         ▼
┌─────────────────┐
│     LGPD        │ ← Termos de consentimento
└─────────────────┘
         │
         ▼
┌─────────────────┐
│ TIPO MANIFESTAÇÃO│← Elogio/Sugestão/Reclamação/Denúncia
└─────────────────┘
         │
         ▼
┌─────────────────┐
│ CATEGORIA DENÚNCIA│← (Apenas para denúncias)
└─────────────────┘
         │
         ▼
┌─────────────────┐
│  DETALHES       │ ← Descrição da manifestação
└─────────────────┘
         │
         ▼
┌─────────────────┐
│  CONFirmaÇÃO    │ ← Resumo + confirmação
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   FINALIZADO    │ ← Protocolo gerado
└─────────────────┘
```

## 🗃️ Estrutura do Banco de Dados

### Tabela: `usuario`
```sql
id, nome, telefone, email, anonimo, lgpd_consentimento, data_consentimento, data_criacao
```

### Tabela: `manifestacao`
```sql
id, tipo, categoria, descricao, resumo, protocolo, data_criacao, usuario_id
```

### Tabela: `chat_state`
```sql
phone_number, current_state, last_update
```

### Tabela: `chat_state_context`
```sql
phone_number, context_key, context_value
```

## 🌐 Endpoints da API

### Webhook Z-API
- **POST** `/webhook/zapi` - Recebe mensagens do WhatsApp

### Health Check
- **GET** `/webhook/health` - Status da aplicação
- **GET** `/webhook/test` - Teste simples

## 📊 Estrutura do Google Sheets

A planilha será automaticamente criada com as colunas:

| Coluna | Descrição |
|--------|-----------|
| Número | ID sequencial |
| Protocolo | Número do protocolo (ex: REC20240115-0001) |
| Data Criação | Data/hora do registro |
| Tipo | Elogio/Sugestão/Reclamação/Denúncia |
| Categoria | Categoria da denúncia (se aplicável) |
| Descrição | Descrição completa |
| Resumo | Resumo automático |
| Usuário | Nome ou "Anônimo" |
| Status | Status do atendimento |

## 🔍 Monitoramento e Debug

### Logs de inicialização
A aplicação grava logs de inicialização no Google Sheets incluindo:
- Tempo de inicialização
- Versão do Java
- Memória disponível
- Profile do Spring

### Debug de credenciais
Em modo desenvolvimento, as credenciais são validadas e debugadas no startup.

### Health Check
```bash
curl http://localhost:8080/webhook/health
```

## 🐛 Solução de Problemas

### Problema: Credenciais do Google não funcionam
**Solução:**
1. Verifique se o `credentials.json` está em `src/main/resources/`
2. Confirme se a service account tem acesso à planilha
3. Verifique os logs de inicialização

### Problema: Mensagens do WhatsApp não chegam
**Solução:**
1. Verifique se o webhook está configurado no Z-API
2. Confirme os tokens no `application.properties`
3. Teste o endpoint `/webhook/test`

### Problema: Erro de banco de dados
**Solução:**
1. Para H2: acesse `http://localhost:8080/h2-console`
2. JDBC URL: `jdbc:h2:file:./data/ouvidoria`
3. Usuário: `sa`, Senha: `password`

## 📈 Deploy em Produção

### 1. Configure variáveis de ambiente
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ouvidoria
export SPRING_DATASOURCE_USERNAME=usuario
export SPRING_DATASOURCE_PASSWORD=senha
export ZAPI_API_TOKEN=seu_token
```

### 2. Build para produção
```bash
mvn clean package -DskipTests
```

### 3. Execute
```bash
java -jar ouvidoria-chatbot-1.0.0.jar
```

## 👥 Desenvolvimento

### Estrutura de pacotes
```
br.com.konekta.ouvidoria/
├── config/          # Configurações
├── controller/      # Controladores REST
├── model/          # Entidades JPA
├── repository/     # Repositórios Spring Data
├── service/        # Lógica de negócio
└── model/enums/    # Enumeradores
```

### Para adicionar novos estados no chatbot
1. Adicione o estado em `EstadoChat`
2. Implemente o handler em `ChatbotService`
3. Atualize o fluxo nos métodos existentes

## 📄 Licença

Este projeto é para uso interno da Konekta.

---

**Desenvolvido por** Rui Carlos Lorenzetti da Silva  
**Suporte**: [konekta.dev@gmail.com]
```