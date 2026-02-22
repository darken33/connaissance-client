# Connaissance Client — Backend API (pour développeurs)

Résumé
------
API backend Java/Spring Boot pour la gestion des fiches de connaissance client (micro-monorepo Maven).

Structure du repository
-----------------------
- Modules Maven (racine `pom.xml`):
  - `connaissance-client-domain` — modèle et logique métier
  - `connaissance-client-db-adapter` — accès persistance (MongoDB)
  - `connaissance-client-cp-adapter` — adaptateur de service tiers (ex. code postaux)
  - `connaissance-client-event-adapter` — publication d'événements (Kafka)
  - `connaissance-client-api` — définition OpenAPI + génération des contrôleurs/DTOs
  - `connaissance-client-app` — application Spring Boot (assemble les modules)

Principaux fichiers
-------------------
- Spécification OpenAPI: `connaissance-client-api/src/main/resources/connaissance-client-api.yaml`
- Dockerfile racine: `Dockerfile` (utilise le jar construit dans `connaissance-client-app/target`)
- Scénarios de test d'intégration: `env-tests/docker/docker-compose.yml` et scripts dans `env-tests/docker` / `env-tests/start.sh`

Prérequis
---------
- Java 21 (le projet cible Java 21)
- Maven (ou utilisez le wrapper `./mvnw` inclus)
- Docker & Docker Compose (pour exécuter les stacks d'intégration)

Compilation et exécution locale
------------------------------
1. Compiler tout le monorepo (clean + tests):

```bash
./mvnw clean install -DskipTests=false
```

2. Lancer l'application (module `connaissance-client-app`) en développement:

```bash
./mvnw -pl connaissance-client-app -am spring-boot:run
```

Explications:
- `-pl connaissance-client-app` : build/launch du module application
- `-am` : build des modules requis en amont

Construire l'exécutable JAR
-------------------------
```bash
./mvnw -DskipTests package -pl connaissance-client-app -am
# Le jar sera dans connaissance-client-app/target/
```

Conteneurisation (Docker)
-------------------------
1. Après avoir produit le jar (`connaissance-client-app/target/connaissance-client-app-<version>.jar`) :

```bash
docker build -t connaissance-client:local .
docker run --rm -p 8080:8080 connaissance-client:local
```

2. Le `Dockerfile` inclut l'import d'un certificat (utilisé pour le runtime); adaptez si nécessaire.

API / Documentation
-------------------
- La définition OpenAPI se trouve dans `connaissance-client-api/src/main/resources/connaissance-client-api.yaml`.
- Le module `connaissance-client-api` utilise l'OpenAPI Generator pour générer les modèles et stubs (configuré dans son `pom.xml`).
- En local, si `springdoc-openapi` est activé, l'UI Swagger est généralement disponible sur `/swagger-ui.html` ou `/swagger-ui/index.html` (port par défaut 8080).

Génération des sources OpenAPI
-----------------------------
Le plugin `openapi-generator-maven-plugin` est configuré dans `connaissance-client-api/pom.xml`. Pour (re)générer les sources :

```bash
./mvnw -pl connaissance-client-api -am generate-sources
```

Tests
-----
- Tests unitaires avec `mvn test`.
- Tests d'intégration (naming convention) exécutés via `maven-failsafe-plugin` (ex: *IT.java).
- Environnement local (MongoDB, Kafka, Schema Registry, …) fourni dans `env-tests/docker/docker-compose.yml` pour exécuter des tests d'intégration manuels ou CI.

Conseils pour le développement
----------------------------
- Utiliser le wrapper `./mvnw` pour garantir la même version de Maven.
- Compiler avec `-DskipTests=false` en CI et exécuter `mvn verify` pour inclure les tests d'intégration.
- Les sources générées OpenAPI se trouvent dans `target/generated-sources` par module; ne pas modifier les fichiers générés directement: préférez les délégations/implémentations dans le code source du module.
- Java 21 est requis; configurez votre IDE (IntelliJ/Eclipse/VSCode) pour utiliser JDK 21.

CI / Qualité
-----------
- Le parent POM inclut des outils configurés (Jacoco, dependency-check, fail-safe). Adapter la configuration de CI pour récupérer les rapports (HTML/XML).

Bonnes pratiques et conventions
------------------------------
- Respecter la séparation domaine / adapters / api / app pour faciliter les tests et l'évolution.
- Les DTOs et controllers exposés proviennent de la génération OpenAPI ; implémenter la logique métier dans le module `connaissance-client-domain` et les adapters.

Contribution
------------
- Fork / branch feature/bugfix
- Respecter les conventions de commit et ouvrir une MR avec description claire
- Ajouter/mettre à jour les tests unitaires et d'intégration selon la portée du changement

Contacts et ressources
----------------------
- Mainteneur principal (contact trouvé dans OpenAPI): pbousquet@sqli.com
- Spécification OpenAPI: `connaissance-client-api/src/main/resources/connaissance-client-api.yaml`

Fichiers utiles
--------------
- [Spécification OpenAPI](connaissance-client-api/src/main/resources/connaissance-client-api.yaml)
- [Dockerfile racine](Dockerfile)
- [Compose d'intégration (env-tests)](env-tests/docker/docker-compose.yml)

