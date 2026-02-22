import requests
import json
from typing import Any, Dict

BASE_URL = "http://localhost:8080"

# Endpoints from the OpenAPI contract
ENDPOINTS = [
    {
        "method": "GET",
        "path": "/v1/connaissance-clients",
        "description": "Liste de toutes les fiches de connaissance client"
    },
    {
        "method": "POST",
        "path": "/v1/connaissance-clients",
        "description": "Création d'une nouvelle fiche de connaissance client",
        "body": {
            "nom": "Test",
            "prenom": "User",
            "ligne1": "1 rue de test",
            "codePostal": "75001",
            "ville": "Paris",
            "situationFamiliale": "CELIBATAIRE",
            "nombreEnfants": 0
        }
    },
    {
        "method": "GET",
        "path": "/v1/connaissance-clients/{id}",
        "description": "Consultation d'une fiche de connaissance client spécifique",
        "params": {"id": "8a9204f5-aa42-47bc-9f04-17caab5deeee"}
    },
    {
        "method": "PUT",
        "path": "/v1/connaissance-clients/{id}",
        "description": "Modification globale d'une fiche de connaissance client",
        "params": {"id": "8a9204f5-aa42-47bc-9f04-17caab5deeee"},
        "body": {
            "nom": "Dupont",
            "prenom": "Marie",
            "ligne1": "25 avenue de la Republique",
            "ligne2": "Appartement 3B",
            "codePostal": "75011",
            "ville": "Paris",
            "situationFamiliale": "MARIE",
            "nombreEnfants": 2
        }
    },
    {
        "method": "DELETE",
        "path": "/v1/connaissance-clients/{id}",
        "description": "Suppression définitive d'une fiche de connaissance client",
        "params": {"id": "8a9204f5-aa42-47bc-9f04-17caab5deeee"}
    },
    {
        "method": "PUT",
        "path": "/v1/connaissance-clients/{id}/adresse",
        "description": "Changement d'adresse du client",
        "params": {"id": "8a9204f5-aa42-47bc-9f04-17caab5deeee"},
        "body": {
            "ligne1": "10 rue de la Paix",
            "codePostal": "75002",
            "ville": "Paris"
        }
    },
    {
        "method": "PUT",
        "path": "/v1/connaissance-clients/{id}/situation",
        "description": "Changement de situation familiale du client",
        "params": {"id": "8a9204f5-aa42-47bc-9f04-17caab5deeee"},
        "body": {
            "situationFamiliale": "MARIE",
            "nombreEnfants": 2
        }
    }
]

def call_endpoint(endpoint: Dict[str, Any]) -> None:
    method = endpoint["method"]
    path = endpoint["path"]
    url = BASE_URL + path
    params = endpoint.get("params", {})
    body = endpoint.get("body", None)
    headers = {"Content-Type": "application/json"}

    # Replace path params if needed
    for k, v in params.items():
        url = url.replace(f"{{{k}}}", str(v))

    print(f"\n--- {method} {url} ---")
    print(f"Description: {endpoint['description']}")
    try:
        if method == "GET":
            resp = requests.get(url, headers=headers)
        elif method == "POST":
            resp = requests.post(url, headers=headers, data=json.dumps(body))
        elif method == "PUT":
            resp = requests.put(url, headers=headers, data=json.dumps(body))
        elif method == "DELETE":
            resp = requests.delete(url, headers=headers)
        else:
            print(f"Méthode non supportée: {method}")
            return
        print(f"Status: {resp.status_code}")
        try:
            print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
        except Exception:
            print(resp.text)
    except Exception as e:
        print(f"Erreur lors de l'appel: {e}")

def main():
    for endpoint in ENDPOINTS:
        call_endpoint(endpoint)

if __name__ == "__main__":
    main()
