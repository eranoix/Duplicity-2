DUPLICITY - Versão Offline
==========================

Esta é uma versão offline do Duplicity, editor de saves do Oxygen Not Included.

COMO USAR:
----------
1. Dê duplo clique em "Duplicity.bat"
2. O navegador abrirá automaticamente com a interface do editor
3. O editor funciona completamente offline - não precisa de internet

REQUISITOS:
-----------
- Node.js instalado (para executar o launcher)
- Navegador moderno (Chrome, Firefox, Edge, etc.)

ARQUIVOS:
---------
- Duplicity.bat - Inicia o editor (duplo clique)
- Duplicity-Debug.bat - Inicia o editor com janela de debug aberta
- duplicity-launcher.cjs - Script Node.js que inicia o servidor local
- dist/ - Pasta com os arquivos da aplicação web (compilada)

NOTAS:
------
- Esta versão registra automaticamente um service worker para funcionar offline
- Os arquivos são servidos de um servidor HTTP local na porta 127.0.0.1
- O log de execução é salvo em "duplicity.log"
