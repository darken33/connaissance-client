# Configuration MCP pour VS Code et GitHub Copilot

## Configuration réalisée

La configuration MCP a été ajoutée dans `.vscode/settings.json` :

```json
{
  "github.copilot.chat.mcp.servers": {
    "connaissance-client": {
      "command": "python",
      "args": [
        "/home/pbousquet/Workspaces/SQLI/POC/signoz/connaissance-client/connaissance-client-mcp/server.py"
      ],
      "env": {
        "VIRTUAL_ENV": "/home/pbousquet/Workspaces/SQLI/POC/signoz/connaissance-client/connaissance-client-mcp/.venv"
      }
    }
  }
}
```

## Comment utiliser

1. **Redémarrez VS Code** pour que la configuration soit prise en compte

2. **Ouvrez GitHub Copilot Chat** (Ctrl+Alt+I ou Cmd+Alt+I)

3. **Utilisez @ pour accéder aux outils MCP** :
   - Tapez `@connaissance-client` dans le chat pour voir les outils disponibles
   - Les outils exposés incluent :
     - Liste des clients
     - Créer un client
     - Récupérer un client
     - Modifier un client
     - Supprimer un client
     - Modifier l'adresse
     - Modifier la situation familiale

4. **Exemples d'utilisation** :
   ```
   @connaissance-client liste tous les clients
   
   @connaissance-client crée un nouveau client Jean Dupont à Paris
   
   @connaissance-client récupère les détails du client avec l'ID xxx
   
   @connaissance-client modifie l'adresse du client xxx
   ```

## Vérification

Pour vérifier que le serveur MCP fonctionne :

```bash
cd connaissance-client-mcp
source .venv/bin/activate
python server.py --help
```

## Prérequis

- ✅ L'API doit être démarrée sur `http://localhost:8080`
- ✅ Le serveur MCP est installé dans `.venv/`
- ✅ VS Code avec GitHub Copilot activé
- ✅ Extension GitHub Copilot Chat

## Architecture

```
VS Code / GitHub Copilot
    ↓
MCP Protocol (STDIO)
    ↓
FastMCP Server (server.py)
    ↓
HTTP/REST
    ↓
Connaissance Client API (localhost:8080)
```

Le serveur MCP fait le pont entre GitHub Copilot et votre API REST, permettant à l'IA d'interagir directement avec vos données clients.
