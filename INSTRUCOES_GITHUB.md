# 📋 Instruções para Criar e Publicar no GitHub

## Passo 1: Criar o Repositório no GitHub

1. Acesse https://github.com
2. Clique no botão **"+"** no canto superior direito e selecione **"New repository"**
3. Preencha os seguintes dados:
   - **Repository name**: `Duplicity-2`
   - **Description**: `Editor de saves offline para Oxygen Not Included`
   - **Visibility**: Escolha **Public** (para GitHub Pages gratuito) ou **Private**
   - **⚠️ IMPORTANTE**: NÃO marque "Add a README file", "Add .gitignore" ou "Choose a license" (já temos esses arquivos)
4. Clique em **"Create repository"**

## Passo 2: Conectar o Repositório Local ao GitHub

Após criar o repositório no GitHub, você verá uma página com instruções. Execute os seguintes comandos no terminal (na pasta do projeto):

```bash
# Verificar se já estamos no repositório git
git status

# Adicionar todos os arquivos (se ainda não foi feito)
git add .

# Fazer o primeiro commit (se ainda não foi feito)
git commit -m "Configuração inicial para GitHub Pages"

# Adicionar o remote do GitHub (substitua SEU_USUARIO pelo seu usuário do GitHub)
git remote add origin https://github.com/SEU_USUARIO/Duplicity-2.git

# Renomear a branch para 'main' se necessário
git branch -M main

# Enviar o código para o GitHub
git push -u origin main
```

## Passo 3: Atualizar o package.json

Antes de fazer o deploy, edite o arquivo `oni-duplicity/package.json`:

1. Abra `oni-duplicity/package.json`
2. Encontre a linha com o script `deploy`
3. Substitua `YOUR_USERNAME` pelo seu nome de usuário do GitHub

Exemplo:
```json
"deploy": "gh-pages -d dist -r https://github.com/arthu/Duplicity-2"
```

## Passo 4: Configurar o GitHub Pages

### Opção A: Usar GitHub Actions (Recomendado)

1. Vá para **Settings** do repositório no GitHub
2. No menu lateral, clique em **Pages**
3. Em **Source**, selecione **"GitHub Actions"**
4. Salve as alterações
5. Faça um novo commit e push para ativar o workflow

### Opção B: Usar Deploy Manual

1. Execute o deploy manual:
   ```bash
   cd oni-duplicity
   npm install
   npm run deploy:build
   ```

2. Vá para **Settings** do repositório no GitHub
3. No menu lateral, clique em **Pages**
4. Em **Source**, selecione **"Deploy from a branch"**
5. Escolha a branch `gh-pages` e a pasta `/ (root)`
6. Clique em **Save**

## Passo 5: Acessar o Editor

Após alguns minutos, o editor estará disponível em:

**https://SEU_USUARIO.github.io/Duplicity-2/**

Substitua `SEU_USUARIO` pelo seu nome de usuário do GitHub.

## ⚠️ Notas Importantes

- Se você já tem arquivos no repositório local, pode ser necessário fazer um `git pull` antes do `git push`
- O GitHub Pages pode levar alguns minutos para publicar as alterações
- Se usar GitHub Actions, verifique a aba **Actions** do repositório para ver o progresso do build
