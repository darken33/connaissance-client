# Specification Driven Development : Prompts pour Connaissance Client

## Constitution

/speckit.constitution En tant qu'architecte technique spécialisé en Java et Spring-Boot, effectue une analyse complète du projet afin d'en déterminer les principes d'architecture et les pratiques de développements afin d'alimenter le constitution.md. Une fois la constitution rédigée, rends-moi la main afin que je procède à sa validation avant de passer à l'étape suivante.​

## Rétro Spécifications​

/speckit.specify En tant qu'architecte technique et fonctionnel spécialisé en Java et Spring-Boot, effectue une analyse complète du code de ce projet afin d'en faire une rétro spécification, appuies toi en premier lieu sur les contrats OpenApi, AsyncApi et Les Modèles de données. Effectue également un schéma d'architecture pour présenter les interactions entre les différents composants. Rends-moi la main, afin que je procède à sa validation avant de passer à l'étape suivante.​

## Nouvelle fonctionnalité​

/speckit.specify Nous allons développer une nouvelle version (2.3.0-SNAPSHOT) avec une nouvelle fonctionnalité. Peux tu proposer en respectant l'ensemble des principes de constitution et de retro-spec précédemment définis un nouvel endpoint de modification partielle du client (nom et/ou prénom) en utilisant le verbe http PATCH ? Une fois la spécification rédigée, rends moi la main afin que je la valide.​

## Clarification​

/speckit.clarify Clarifies la spécification ci jointe si nécessaire (nouvel endpoint de modification client avec le verbe http PATCH).​

## Définir le plan​

/speckit.plan Crées un plan pour la spécification ci-jointe. Une fois le plan établi rends moi la main afin que je le valide.​

## Transformer le plan en tâches​

/speckit.tasks Transforme le plan ci-joint en tâches. Une fois les tâches établies rends moi la main afin que je les valide.​

## Implémentation

/speckit.implement Démarres la mise en oeuvre par phases. Après chaque phase propose moi une étape de validation. Une fois validée, effectues un commit avant de passer à la phase suivante.​