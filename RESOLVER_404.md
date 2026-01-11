# 🔧 Como Resolver o Erro 404 do GitHub Pages

## ⚠️ O que o erro 404 significa?

O erro 404 "There isn't a GitHub Pages site here" significa que o site ainda não foi publicado. Isso é normal se:
- O workflow ainda não rodou
- O workflow está rodando (aguarde)
- O workflow falhou

## 📋 Passo a Passo para Resolver

### 1️⃣ Verificar o Status do Workflow

1. Acesse a aba **Actions** do seu repositório:
   **https://github.com/eranoix/Duplicity-2/actions**

2. Procure pelo workflow **"Build and Deploy to GitHub Pages"**

3. Verifique o status:
   - ✅ **Verde (✓)**: Workflow completou com sucesso - aguarde alguns minutos para o site ficar disponível
   - 🟡 **Amarelo (○)**: Workflow está rodando - aguarde
   - ❌ **Vermelho (✗)**: Workflow falhou - veja os logs para identificar o erro

### 2️⃣ Disparar o Workflow Manualmente (se necessário)

Se o workflow não aparecer ou não estiver rodando:

1. Na aba **Actions**, clique no workflow **"Build and Deploy to GitHub Pages"** no menu lateral
2. Clique no botão **"Run workflow"** (no topo direito)
3. Selecione a branch **"main"**
4. Clique em **"Run workflow"**
5. Aguarde a conclusão (pode levar 2-5 minutos)

### 3️⃣ Verificar os Logs (se o workflow falhou)

Se o workflow falhou:

1. Clique no workflow que falhou
2. Clique no job **"build-and-deploy"**
3. Revise os logs para identificar o erro
4. Os erros mais comuns são:
   - Problemas de dependências (npm install falha)
   - Problemas de build (npm run build falha)
   - Problemas de permissões

### 4️⃣ Aguardar a Publicação

Após o workflow completar com sucesso:

- O GitHub Pages pode levar **alguns minutos** (até 10 minutos) para propagar
- Aguarde e tente acessar novamente: **https://eranoix.github.io/Duplicity-2/**

### 5️⃣ Verificar as Configurações do GitHub Pages

Certifique-se de que:

1. Acesse: **https://github.com/eranoix/Duplicity-2/settings/pages**
2. Verifique se **"GitHub Actions"** está selecionado como **Source**
3. Se não estiver, selecione e salve

## 🔍 Verificações Rápidas

- ✅ Workflow está configurado: `.github/workflows/deploy.yml`
- ✅ GitHub Pages está configurado para usar GitHub Actions
- ✅ Workflow completou com sucesso?
- ⏳ Aguardou alguns minutos após o workflow completar?

## 💡 Dica

O GitHub Pages pode levar alguns minutos para ficar disponível mesmo após o workflow completar. Se o workflow está verde mas o site ainda mostra 404, aguarde mais alguns minutos e tente novamente.

## 🆘 Se Ainda Não Funcionar

Se após seguir todos os passos o site ainda não funcionar, verifique:
1. Se o repositório é público (GitHub Pages gratuito só funciona em repositórios públicos)
2. Se há algum erro nos logs do workflow
3. Tente fazer um pequeno commit para disparar o workflow novamente
