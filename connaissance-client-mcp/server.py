from typing import Any, Dict
import httpx
from fastmcp import FastMCP

# Create an HTTP client for your API
client = httpx.AsyncClient(base_url="http://localhost:8080")

# Load your OpenAPI spec 
openapi_spec = httpx.get("http://localhost:8080/v3/api-docs").json()

# Create the MCP server
mcp = FastMCP("Connaissance Client API Server") 

#FastMCP.from_openapi(
#    openapi_spec=openapi_spec,
#    client=client,
#    name="Connaissance Client API Server"
#)

# Remplacez l'URL par celle de votre API si besoin
BASE_URL = "http://localhost:8080"

@mcp.tool(description="""
          Récupère la liste des clients (Personnes Physiques) disponibles. 
          Cas d'usage : 
            - Affichage de la liste des clients pour sélection 
            - Export des données clients 
            - Recherche globale dans la base clients 
          Réponse : 
            - Retourne un tableau de clients avec toutes leurs informations 
            - Liste vide si aucun client n'est enregistré
          Une fiche client contient les champs suivants : 
            - id : Identifiant unique du client 
            - nom : Nom de famille du client 
            - prenom : Prénom du client 
            - adresse : Adresse postale complète du client composée de :
              - ligne1 : Première ligne de l'adresse 
              - ligne2 : Deuxième ligne de l'adresse (optionnelle)
              - codePostal : Code postal de la résidence du client
              - ville : Ville de résidence du client 
            - situationFamiliale : Situation familiale du client composée de :
              - situationFamiliale : Statut marital (Célibataire, Marié, Divorcé, Veuf)
              - nombreEnfants : Nombre d'enfants à charge 
          Validité des données : 
            - Pour obtenir des informations en temps réel il faut appeler cette API en premier avant tout traitement.
          """)
def get_clients():
    # Endpoint à adapter selon votre API (ex: /clients ou /connaissance-clients)
    url = f"{BASE_URL}/v1/connaissance-clients"
    response = httpx.get(url)
    response.raise_for_status()
    clients = response.json()
    return clients

@mcp.tool(description="""
          Modifie uniquement l'adresse d'un client existant. 
          Cas d'usage : 
            - Déménagement du client 
            - Correction d'une adresse erronée 
            - Mise à jour suite à un changement de domicile
          Paramètres : 
            - id_client : Identifiant unique du client à modifier au format UUID 
            - new_address : Nouvelle adresse du client sous forme de dictionnaire contenant : 
              - ligne1 : Première ligne de l'adresse 
              - ligne2 : Deuxième ligne de l'adresse (optionnelle)
              - codePostal : Code postal de la résidence du client
              - ville : Ville de résidence du client 
          Réponse : 
            - Retourne les données du client avec toutes ses informations :
              - id : Identifiant unique du client au format UUID
              - nom : Nom de famille du client 
              - prenom : Prénom du client 
              - adresse : Adresse postale complète du client composée de :
                - ligne1 : Première ligne de l'adresse 
                - ligne2 : Deuxième ligne de l'adresse (optionnelle)
                - codePostal : Code postal de la résidence du client
                - ville : Ville de résidence du client 
              - situationFamiliale : Situation familiale du client composée de :
                - situationFamiliale : Statut marital (Célibataire, Marié, Divorcé, Veuf)
                - nombreEnfants : Nombre d'enfants à charge 
          Validation : 
            - Le client doit exister (erreur 404 sinon) 
            - L'adresse est validée via l'API IGN (code postal/ville cohérents) 
            - En cas d'échec de validation externe, erreur 422 
          Comportement : 
            - Seule l'adresse est modifiée (nom, prénom, situation inchangés) 
            - Un événement Kafka est publié pour notifier le changement d'adresse 
            - Audit trail avec traçabilité complète 
            - Validation externe avec circuit breaker (résilience) 
          Performance : 
            - Temps de réponse typique : < 2s (avec validation API IGN) 
            - Circuit breaker actif pour l'API IGN (3 échecs → skip 60s)          
          """)
def patch_address(id_client: str, new_address: Dict[str, Any]):
    # Endpoint à adapter selon votre API (ex: /clients ou /connaissance-clients)
    url = f"{BASE_URL}/v1/connaissance-clients/{id_client}/adresse"
    body = {
      "ligne1": new_address.get("ligne1", ""),
      "codePostal": new_address.get("codePostal", ""),
      "ville": new_address.get("ville", "")
    }
    if "ligne2" in new_address and new_address["ligne2"] != "":
        body["ligne2"] = new_address["ligne2"]
    
    headers = {"Content-Type": "application/json"}

    response = httpx.patch(url, json=body, headers=headers)
    response.raise_for_status()
    client = response.json()
    return client

@mcp.tool(description="""
          Modifie uniquement la situation familiale et le nombre d'enfants d'un client existant. 
          Cas d'usage : 
            - Changement d'état civil (mariage, divorce, veuvage) 
            - Naissance ou adoption d'enfant(s) 
            - Mise à jour suite à un événement de vie 
            - Correction d'informations familiales erronées 
          Paramètres :
            - id_client : Identifiant unique du client à modifier au format UUID 
            - new_situation : Nouvelle situation familiale du client sous forme de dictionnaire contenant : 
              - situationFamiliale : Statut marital (CELIBATAIRE, MARIE, DIVORCE, VEUF, PACSE)
              - nombreEnfants : Nombre d'enfants à charge (entier entre 0 et 20)
          Réponse : 
            - Retourne les données du client avec toutes ses informations :
              - id : Identifiant unique du client au format UUID
              - nom : Nom de famille du client 
              - prenom : Prénom du client 
              - adresse : Adresse postale complète du client composée de :
                - ligne1 : Première ligne de l'adresse 
                - ligne2 : Deuxième ligne de l'adresse (optionnelle)
                - codePostal : Code postal de la résidence du client
                - ville : Ville de résidence du client 
              - situationFamiliale : Situation familiale du client composée de :
                - situationFamiliale : Statut marital (Célibataire, Marié, Divorcé, Veuf)
                - nombreEnfants : Nombre d'enfants à charge 
          Validation : 
            - Le client doit exister (erreur 404 sinon) 
            - La situation familiale doit être valide (CELIBATAIRE, MARIE, DIVORCE, VEUF, PACSE) 
            - Le nombre d'enfants doit être entre 0 et 20 
            - Cohérence métier : un célibataire peut avoir des enfants 
          Comportement : 
            - Seule la situation familiale est modifiée (nom, prénom, adresse inchangés) 
            - Aucun événement Kafka n'est publié (pas de changement d'adresse) 
            - Audit trail avec traçabilité complète 
            - Opération transactionnelle 
          Règles métier : 
            - Un client célibataire peut avoir des enfants (garde alternée, adoption, etc.) 
            - Le nombre d'enfants peut être 0 même si marié 
            - La situation PACSE est acceptée (Pacte Civil de Solidarité) 
          Performance : 
            - Temps de réponse typique : < 100ms 
            - Pas de validation externe requise
          """)
def patch_situation_familiale(id_client: str, new_situation: Dict[str, Any]):
    # Endpoint à adapter selon votre API (ex: /clients ou /connaissance-clients)
    url = f"{BASE_URL}/v1/connaissance-clients/{id_client}/situation"
    body = {
      "situationFamiliale": new_situation.get("situationFamiliale", ""),
      "nombreEnfants": new_situation.get("nombreEnfants", 0)
    }
    
    headers = {"Content-Type": "application/json"}

    response = httpx.patch(url, json=body, headers=headers)
    response.raise_for_status()
    client = response.json()
    return client

@mcp.tool(description="""
          Crée une nouvelle fiche de connaissance client dans le système. 
          Cas d'usage : 
            - Enregistrement d'un nouveau client lors de l'onboarding 
            - Import de données clients depuis un système externe 
            - Saisie manuelle par un conseiller 
          Paramètres :
            - new_client : Nouveau client à enregistrer en base
              - nom : Nom de famille du client 
              - prenom : Prénom du client 
              - ligne1 : Première ligne de l'adresse 
              - ligne2 : Deuxième ligne de l'adresse (optionnelle)
              - codePostal : Code postal de la résidence du client
              - ville : Ville de résidence du client 
              - situationFamiliale : Statut marital (CELIBATAIRE, MARIE, DIVORCE, VEUF, PACSE)
              - nombreEnfants : Nombre d'enfants à charge (entier entre 0 et 20)
          Réponse : 
            - Retourne les données du client avec toutes ses informations :
              - id : Identifiant unique du client au format UUID
              - nom : Nom de famille du client 
              - prenom : Prénom du client 
              - adresse : Adresse postale complète du client composée de :
                - ligne1 : Première ligne de l'adresse 
                - ligne2 : Deuxième ligne de l'adresse (optionnelle)
                - codePostal : Code postal de la résidence du client
                - ville : Ville de résidence du client 
              - situationFamiliale : Situation familiale du client composée de :
                - situationFamiliale : Statut marital (Célibataire, Marié, Divorcé, Veuf)
                - nombreEnfants : Nombre d'enfants à charge 
          Validation : 
            - Tous les champs obligatoires doivent être fournis 
            - Le format des données est vérifié (email, code postal, etc.) 
            - La cohérence des informations familiales est contrôlée 
          Résultat : 
            - La fiche créée est retournée avec son ID généré 
            - Un événement de création est émis pour notification 
          Règles métier : 
            - Un client ne peut avoir qu'une seule fiche active 
            - Les informations personnelles sont chiffrées en base
          """)
def post_client(new_client: Dict[str, Any]):
    # Endpoint à adapter selon votre API (ex: /clients ou /connaissance-clients)
    url = f"{BASE_URL}/v1/connaissance-clients"
    body = {
      "nom": new_client.get("nom", ""),
      "prenom": new_client.get("prenom", ""),
      "ligne1": new_client.get("ligne1", ""),
      "codePostal": new_client.get("codePostal", ""),
      "ville": new_client.get("ville", ""),
      "situationFamiliale": new_client.get("situationFamiliale", ""),
      "nombreEnfants": new_client.get("nombreEnfants", 0)
    }
    if "ligne2" in new_client and new_client["ligne2"] != "":
        body["ligne2"] = new_client["ligne2"]
    
    headers = {"Content-Type": "application/json"}

    response = httpx.post(url, json=body, headers=headers)
    response.raise_for_status()
    client = response.json()
    return client

if __name__ == "__main__":
    mcp.run(transport="http", host="127.0.0.1", port=8000)