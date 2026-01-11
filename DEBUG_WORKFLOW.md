# 🔍 Como Debugar o Workflow que Está Falhando

## ⚠️ Problema: Todos os Workflows Estão Falhando

Todos os 5 workflows falharam com ícone vermelho (❌). Precisamos investigar os logs para identificar o erro.

## 📋 Passo a Passo para Ver os Logs do Erro

### 1️⃣ Acessar os Logs do Workflow

1. Acesse: **https://github.com/eranoix/Duplicity-2/actions**

2. Clique no workflow mais recente que falhou (o primeiro da lista)

3. Clique no job **"build-and-deploy"** (deve aparecer com ❌ vermelho)

4. Você verá os logs detalhados de cada etapa:
   - ✅ Checkout
   - ✅ Setup Node.js
   - ⚠️ Install dependencies
   - ⚠️ Build
   - ⚠️ Setup Pages
   - ⚠️ Upload artifact
   - ⚠️ Deploy to GitHub Pages

### 2️⃣ Identificar a Etapa que Falhou

Expanda cada etapa e procure por:
- ❌ Mensagens de erro em vermelho
- ⚠️ Warnings (avisos)
- Mensagens que começam com "Error:", "Failed:", "npm ERR!"

### 3️⃣ Erros Mais Comuns e Soluções

#### Erro: "npm ci" falha
- **Causa**: Problemas com package-lock.json ou dependências
- **Solução**: Verificar se package-lock.json está no repositório

#### Erro: "npm run build" falha
- **Causa**: Erro de compilação do TypeScript/React
- **Solução**: Verificar erros de sintaxe ou dependências faltando

#### Erro: "Path './oni-duplicity/dist' does not exist"
- **Causa**: O build não gerou a pasta dist
- **Solução**: Verificar se o build está configurado corretamente

#### Erro: Permissões do GitHub Pages
- **Causa**: Permissões não configuradas corretamente
- **Solução**: Verificar se GitHub Pages está configurado para usar GitHub Actions

## 🔧 Ações Imediatas

### Verificar os Logs

1. Clique no workflow mais recente que falhou
2. Clique no job "build-and-deploy"
3. Expanda cada etapa e copie os erros que aparecerem
4. Me envie os erros para eu ajudar a resolver

### Verificações Rápidas

- ✅ O arquivo `.github/workflows/deploy.yml` existe?
- ✅ O arquivo `oni-duplicity/package.json` existe?
- ✅ O arquivo `oni-duplicity/package-lock.json` existe?
- ✅ GitHub Pages está configurado para usar "GitHub Actions"?

## 💡 Próximos Passos

Depois de identificar o erro nos logs:
1. Me envie o erro específico
2. Vou ajudar a corrigir o problema
3. Faremos um novo commit para testar novamente
