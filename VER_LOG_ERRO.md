# 🔍 Como Ver os Logs do Erro do Workflow

## ⚠️ IMPORTANTE: Preciso ver os logs do erro!

Todos os workflows estão falhando. Para resolver, preciso ver os logs de erro.

## 📋 Passo a Passo para Ver os Logs

### 1️⃣ Acessar o Workflow que Falhou

1. Acesse: **https://github.com/eranoix/Duplicity-2/actions**

2. Clique no workflow mais recente que falhou (o primeiro da lista, "Build and Deploy to GitHub Pages #5")

### 2️⃣ Ver os Logs do Job

3. Clique no job **"build-and-deploy"** (deve aparecer com ❌ vermelho)

### 3️⃣ Identificar a Etapa que Falhou

4. Você verá uma lista de etapas (steps):
   - ✅ Checkout
   - ✅ Setup Node.js
   - ⚠️ Install dependencies
   - ⚠️ Build
   - ⚠️ Setup Pages
   - ⚠️ Upload artifact
   - ⚠️ Deploy to GitHub Pages

5. Expanda cada etapa clicando nela (especialmente as que têm ⚠️ ou ❌)

6. Procure por:
   - ❌ Mensagens em vermelho
   - Palavras como "Error:", "Failed:", "npm ERR!"
   - Linhas que começam com "##[error]"

### 4️⃣ Copiar o Erro

7. Selecione e copie as mensagens de erro (especialmente a parte que mostra o erro)

8. Me envie essas mensagens de erro para eu ajudar a resolver

## 🎯 O que Procurar

Os erros mais comuns são:

- **"npm ERR! code ENOENT"** - Arquivo ou diretório não encontrado
- **"npm ERR! Cannot read package.json"** - Problema com package.json
- **"Error: Cannot find module"** - Módulo não encontrado
- **"Path does not exist"** - Caminho não existe
- **"Permission denied"** - Problema de permissões

## 📸 Alternativa: Tirar Screenshot

Se preferir, tire um screenshot da parte dos logs que mostra o erro e me envie!
