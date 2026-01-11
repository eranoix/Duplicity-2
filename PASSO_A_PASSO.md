# 🚀 Passo a Passo - Criar Repositório no GitHub e Publicar

## ⚠️ IMPORTANTE: O que você precisa fazer manualmente

Não é possível criar o repositório no GitHub automaticamente. Você precisa fazer isso manualmente através do site do GitHub.

## 📋 Passos para Publicar no GitHub

### 1️⃣ Criar o Repositório no GitHub (FAZER AGORA)

1. Acesse: https://github.com/new
2. Preencha os dados:
   - **Repository name**: `Duplicity-2`
   - **Description**: `Editor de saves offline para Oxygen Not Included`
   - **Visibility**: Escolha **Public** (recomendado para GitHub Pages gratuito)
   - **⚠️ IMPORTANTE**: NÃO marque nenhuma opção:
     - ❌ NÃO marque "Add a README file"
     - ❌ NÃO marque "Add .gitignore"  
     - ❌ NÃO marque "Choose a license"
   - (Já temos esses arquivos no projeto)
3. Clique no botão verde **"Create repository"**

### 2️⃣ Conectar o Repositório Local ao GitHub

Após criar o repositório, o GitHub mostrará uma página com instruções. **NÃO** siga essas instruções completas, pois já temos arquivos no repositório local.

Em vez disso, execute no terminal (na pasta do projeto `Duplicity-2`):

```bash
# Adicionar o remote do GitHub (substitua SEU_USUARIO pelo seu usuário do GitHub)
git remote add origin https://github.com/SEU_USUARIO/Duplicity-2.git

# Verificar se foi adicionado corretamente
git remote -v

# Enviar o código para o GitHub
git push -u origin main
```

**Exemplo:** Se seu usuário for `arthu`, o comando seria:
```bash
git remote add origin https://github.com/arthu/Duplicity-2.git
git push -u origin main
```

### 3️⃣ Configurar o GitHub Pages

Depois que o código estiver no GitHub:

1. Vá para **Settings** do seu repositório (ícone de engrenagem no topo)
2. No menu lateral esquerdo, clique em **Pages**
3. Em **Source**, selecione **"GitHub Actions"**
4. Salve (se já não estiver selecionado)

### 4️⃣ Ativar o Workflow

O workflow do GitHub Actions será ativado automaticamente quando você fizer o primeiro push. Para verificar:

1. Vá para a aba **Actions** do seu repositório
2. Você verá o workflow "Build and Deploy to GitHub Pages" rodando
3. Aguarde a conclusão (pode levar alguns minutos)

### 5️⃣ Acessar o Editor

Após o workflow completar com sucesso (verde), o editor estará disponível em:

**https://SEU_USUARIO.github.io/Duplicity-2/**

Substitua `SEU_USUARIO` pelo seu nome de usuário do GitHub.

## ❓ Precisa de Ajuda?

Se encontrar algum erro, me avise e eu ajudo a resolver!
