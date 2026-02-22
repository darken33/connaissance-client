# Connaissance Client MCP Server

Serveur MCP (Model Context Protocol) pour l'API Connaissance Client, permettant aux LLMs comme Claude d'interagir avec l'API REST via des outils structurés.

## 🚀 Fonctionnalités

### Tools disponibles

- **list_clients()** - Liste tous les clients
- **get_client(client_id)** - Récupère un client spécifique
- **create_client(...)** - Crée un nouveau client avec validation d'adresse
- **update_client(client_id, ...)** - Modification complète d'un client
- **update_address(client_id, ...)** - Modification uniquement de l'adresse
- **update_situation(client_id, ...)** - Modification uniquement de la situation familiale
- **delete_client(client_id)** - Suppression définitive d'un client

### Resources disponibles

- **api://connaissance-client/openapi** - Contrat OpenAPI complet
- **api://connaissance-client/health** - État de santé de l'API

### Prompts disponibles

- **create_test_client** - Exemple de création de client
- **search_client** - Exemple de recherche de client

## 📦 Installation

### Option 1: Avec uv (recommandé)

```bash
cd connaissance-client-mcp
uv venv
source .venv/bin/activate  # ou .venv\Scripts\activate sur Windows
uv pip install -e .
```

### Option 2: Avec pip

```bash
cd connaissance-client-mcp
python -m venv .venv
source .venv/bin/activate  # ou .venv\Scripts\activate sur Windows
pip install -e .
```

## 🔧 Configuration

### Pour Claude Desktop

Ajoutez dans `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) ou `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "connaissance-client": {
      "command": "python",
      "args": [
        "/chemin/absolu/vers/connaissance-client-mcp/server.py"
      ],
      "env": {
        "PYTHONPATH": "/chemin/absolu/vers/connaissance-client-mcp"
      }
    }
  }
}
```

### Configuration de l'URL de l'API

Par défaut, le serveur se connecte à `http://localhost:8080`. Pour modifier:

```python
# Dans server.py
API_BASE_URL = "http://votre-api:8080"
```

## 🏃 Utilisation

### Test en ligne de commande (mode stdio)

```bash
python server.py
```

Le serveur attend des messages MCP sur stdin et répond sur stdout.

### Test avec mcp-client

```bash
uv pip install mcp
mcp dev server.py
```

### Utilisation avec Claude Desktop

1. Démarrez l'API Connaissance Client:
   ```bash
   cd /home/pbousquet/Workspaces/SQLI/POC/signoz/connaissance-client
   mvn spring-boot:run -pl connaissance-client-app
   ```

2. Vérifiez que l'API est accessible:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. Lancez Claude Desktop - le serveur MCP sera automatiquement démarré

4. Dans Claude, vous pouvez maintenant:
   - "Liste tous les clients de la base"
   - "Crée un nouveau client avec le nom Dupont, prénom Jean, à Paris"
   - "Modifie l'adresse du client [UUID] pour 456 rue Victor Hugo, 33000 Bordeaux"
   - "Supprime le client [UUID]"

## 🧪 Tests

### Tester les tools individuellement

```python
from server import mcp, list_clients, get_client, create_client

# Liste les clients
clients = await list_clients()
print(clients)

# Crée un client de test
client = await create_client(
    nom="Test",
    prenom="User",
    ligne1="123 Test Street",
    codePostal="75001",
    ville="Paris",
    situationFamiliale="CELIBATAIRE",
    nombreEnfants=0
)
print(client)
```

### Tester avec curl

```bash
# Liste les clients
curl http://localhost:8080/v1/connaissance-clients

# Crée un client
curl -X POST http://localhost:8080/v1/connaissance-clients \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "ligne1": "123 rue de la Paix",
    "codePostal": "75001",
    "ville": "Paris",
    "situationFamiliale": "MARIE",
    "nombreEnfants": 2
  }'
```

## 📝 Exemples de requêtes Claude

### Création d'un client

```
Crée un nouveau client avec:
- Nom: Martin
- Prénom: Sophie
- Adresse: 48 rue Bauducheu, 33800 Bordeaux
- Situation: MARIE
- Enfants: 3
```

### Recherche et modification

```
Liste tous les clients, trouve celui qui s'appelle "Dupont",
et change son adresse pour 10 avenue des Champs Elysées, 75008 Paris
```

### Mise à jour de situation

```
Pour le client [UUID], change sa situation familiale en DIVORCE
et mets le nombre d'enfants à 2
```

## 🔒 Sécurité

- Le serveur MCP ne gère pas l'authentification (à ajouter si nécessaire)
- Les validations sont effectuées côté API (format, cohérence des données)
- L'API valide les adresses via l'API IGN
- Les opérations de suppression sont irréversibles

## 🐛 Debug

### Logs de connexion

```bash
# Voir les logs Claude Desktop (macOS)
tail -f ~/Library/Logs/Claude/mcp*.log

# Windows
type %APPDATA%\Claude\logs\mcp*.log
```

### Erreurs courantes

**"Connection refused"**
- Vérifiez que l'API tourne sur localhost:8080
- `curl http://localhost:8080/actuator/health`

**"Module not found"**
- Vérifiez que les dépendances sont installées: `uv pip list`
- Réinstallez: `uv pip install -e .`

**"Invalid address"**
- L'API valide les adresses via l'API IGN
- Vérifiez que le code postal et la ville correspondent

## 📚 Documentation

- [FastMCP Documentation](https://github.com/jlowin/fastmcp)
- [Model Context Protocol Spec](https://modelcontextprotocol.io/)
- [API Connaissance Client - OpenAPI](http://localhost:8080/v3/api-docs)

## 🔄 Développement

### Structure du projet

```
connaissance-client-mcp/
├── server.py          # Serveur MCP principal
├── pyproject.toml     # Configuration et dépendances
└── README.md          # Documentation
```

### Ajouter un nouveau tool

```python
@mcp.tool()
async def my_new_tool(param: str) -> str:
    """
    Description du tool pour le LLM.
    
    Args:
        param: Description du paramètre
    
    Returns:
        Description du retour
    """
    # Implémentation
    data = await api_request("GET", f"/endpoint/{param}")
    return data
```

### Tests automatiques

```bash
# TODO: Ajouter pytest
uv pip install pytest pytest-asyncio httpx
pytest
```

## 📄 Licence

Même licence que le projet Connaissance Client.
