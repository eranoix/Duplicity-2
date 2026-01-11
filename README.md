# Duplicity-2

Editor de saves offline para **Oxygen Not Included** (ONI), baseado em React e TypeScript.

## 📋 Sobre

Duplicity é um editor de saves baseado em web para o jogo Oxygen Not Included. Esta versão foi configurada para funcionar online através do GitHub Pages, permitindo que qualquer pessoa acesse o editor diretamente pelo navegador sem necessidade de instalação.

## 🌐 Acesso Online

O editor está disponível em: **https://SEU_USUARIO.github.io/Duplicity-2/**

> **Nota:** Substitua `SEU_USUARIO` pelo seu nome de usuário do GitHub após configurar o GitHub Pages.

## 🚀 Como Publicar no GitHub Pages

### Pré-requisitos

- Node.js instalado (versão 14 ou superior)
- Conta no GitHub
- Repositório criado no GitHub chamado `Duplicity-2`

### Passos para Deploy

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/SEU_USUARIO/Duplicity-2.git
   cd Duplicity-2
   ```

2. **Instale as dependências:**
   ```bash
   cd oni-duplicity
   npm install
   ```

3. **Atualize o script de deploy no `package.json`:**
   
   Abra `oni-duplicity/package.json` e substitua `YOUR_USERNAME` pelo seu usuário do GitHub no script `deploy`.

4. **Escolha o método de deploy:**

   **Opção A - Deploy Manual:**
   ```bash
   npm run deploy:build
   ```
   
   Isso irá:
   - Compilar o projeto para produção
   - Fazer deploy para a branch `gh-pages` do seu repositório
   
   **Opção B - Deploy Automático (Recomendado):**
   - Use o GitHub Actions workflow (veja seção abaixo)

5. **Configure o GitHub Pages:**
   
   - Vá para as configurações do seu repositório no GitHub
   - Navegue até **Settings > Pages**
   - Em **Source**, selecione:
     - **Deploy from a branch**: Se usar deploy manual via `npm run deploy` (selecione branch `gh-pages` e pasta `/ (root)`)
     - **GitHub Actions**: Se usar o workflow automático (recomendado)
   - Salve as alterações

6. **Acesse o editor:**
   
   Após alguns minutos, o editor estará disponível em:
   `https://SEU_USUARIO.github.io/Duplicity-2/`

### 🔄 Deploy Automático (GitHub Actions)

O projeto inclui um workflow do GitHub Actions (`.github/workflows/deploy.yml`) que faz build e deploy automático sempre que você fizer push para a branch `main` ou `master`.

Para usar:
1. Certifique-se de que o workflow está habilitado (deve estar por padrão)
2. Faça push das alterações para a branch `main` ou `master`
3. O GitHub Actions fará o build e deploy automaticamente
4. Aguarde a conclusão do workflow nas **Actions** do seu repositório

## 🛠️ Desenvolvimento Local

Para rodar o projeto localmente durante o desenvolvimento:

```bash
cd oni-duplicity
npm install
npm start
```

O projeto estará disponível em `http://localhost:8080`

## 📦 Estrutura do Projeto

```
Duplicity-2/
├── oni-duplicity/          # Código-fonte da aplicação React
│   ├── src/                # Código-fonte TypeScript/React
│   ├── dist/               # Build de produção (gerado)
│   ├── package.json        # Dependências e scripts
│   └── webpack.config.js   # Configuração do Webpack
├── node/                   # Dependências do parser de saves
├── assets/                 # Assets do editor (imagens, etc.)
└── README.md              # Este arquivo
```

## 🔧 Tecnologias Utilizadas

- **React 16.11** - Framework frontend
- **TypeScript** - Linguagem principal
- **Redux + Redux Saga** - Gerenciamento de estado
- **Material-UI** - Componentes de interface
- **React Router** - Roteamento (HashRouter)
- **Webpack** - Bundler
- **Workbox** - Service Worker para cache offline

## 📝 Funcionalidades

- ✅ Edição de duplicantes (atributos, habilidades, aparência)
- ✅ Gerenciamento de criaturas
- ✅ Edição de gêiseres
- ✅ Gerenciamento de materiais
- ✅ Editor RAW (JSON)
- ✅ Modo offline (Service Worker)
- ✅ Interface multi-idioma

## 📄 Licença

MIT License - Veja o arquivo LICENSE para mais detalhes.

## 🤝 Contribuindo

Contribuições são bem-vindas! Sinta-se livre para abrir issues e pull requests.

## 📚 Referências

Este projeto é baseado no projeto original [oni-duplicity](https://github.com/RoboPhred/oni-duplicity) de RoboPhred.
