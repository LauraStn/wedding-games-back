# Wedding Games API

Backend du socle applicatif d'une application privée d'animations de mariage. Ce dépôt contient
uniquement l'API, la logique métier, la sécurité, les migrations et les tests. Le frontend
(React/TypeScript) vit dans un dépôt séparé.

## Architecture

Architecture par domaine (package-by-feature), chaque package séparant contrôleurs REST, DTO,
services, entités JPA et repositories :

```text
src/main/java/com/weddinggames/backend/
  common/         BaseEntity, exceptions metier, format d'erreur commun, generateur de jeton opaque
  configuration/  OpenAPI, CORS, horloge, bootstrap (evenement + premier admin), seed dev/test
  security/       Session opaque (cookie HttpOnly), filtre d'authentification, roles
  event/          WeddingEvent (configuration publique de l'evenement)
  participant/    Participant (invites, maries, organisation)
  invitation/     Jetons d'invitation opaques (QR code), resolution, confirmation
  staff/          Comptes de l'organisation (BCrypt), login, gestion des roles
  session/        Endpoint transverse "session courante" (participant OU staff)
  exclusion/      Exclusions de matchmaking (PairingExclusion, PairingConstraintService)
  lobby/          Salon d'attente, presence, heartbeat
```

Les entités JPA ne sont jamais retournées directement par l'API : chaque contrôleur convertit vers
un DTO explicite (record Java).

## Prérequis

- Java 21 (LTS)
- Maven (le Maven Wrapper `./mvnw` est fourni, aucune installation globale requise si Java est present)
- Docker (pour PostgreSQL en developpement et pour les tests Testcontainers)

## Démarrage de PostgreSQL (développement)

```bash
cp .env.example .env
docker compose up -d
```

Cela démarre uniquement PostgreSQL (voir `compose.yaml`). Aucune autre dépendance (Redis, etc.)
n'est nécessaire pour cette phase.

## Lancement du backend

```bash
export $(grep -v '^#' .env | xargs)   # ou configurez les variables autrement
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Au démarrage (profil `dev` ou `test`), le socle crée automatiquement de façon idempotente :

- un premier événement et un premier compte `ADMIN`, si `APP_BOOTSTRAP_EVENT_SLUG` et
  `APP_BOOTSTRAP_ADMIN_USERNAME`/`APP_BOOTSTRAP_ADMIN_PASSWORD` sont renseignés ;
- des données de démonstration (`DevSeedDataRunner`) : un événement `seed-wedding` et les
  participants **Jessika Dijoux**, **Sandrine Santin**, **Patrick Santin**, avec deux exclusions
  `HARD` (Jessika/Sandrine et Jessika/Patrick) exprimées uniquement par UUID de participant —
  jamais par comparaison de noms dans le code.

## Variables d'environnement

Voir `.env.example` pour la liste complète et des valeurs par défaut sûres. Principales
catégories :

| Groupe | Variables | Rôle |
|---|---|---|
| Base de données | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL |
| Serveur | `SERVER_PORT`, `SPRING_PROFILES_ACTIVE` | Port HTTP, profil actif (`dev`/`test`/`prod`) |
| CORS | `APP_CORS_ALLOWED_ORIGINS` | Origine(s) frontend autorisée(s), séparées par des virgules |
| Cookie de session | `APP_SESSION_COOKIE_*` | Nom, durée de vie, `Secure`, `SameSite`, domaine |
| OpenAPI | `SWAGGER_ENABLED` | Active/désactive Swagger UI et `/v3/api-docs` |
| Bootstrap | `APP_BOOTSTRAP_ADMIN_*`, `APP_BOOTSTRAP_EVENT_*` | Premier admin / premier événement |
| Invitations | `APP_INVITATION_BASE_URL` | Base d'URL du frontend pour les liens d'invitation |

Aucun secret réel n'est présent dans le dépôt.

## Profils

- **dev** : Swagger activé, cookie non `Secure` (HTTP local), données de démonstration seedées,
  logs verbeux.
- **test** : utilisé par les tests (Testcontainers), Swagger désactivé, mêmes données de
  démonstration que `dev` pour permettre les tests métier (Jessika/Sandrine/Patrick).
- **prod** : Swagger désactivé, cookie `Secure` obligatoire, `server.forward-headers-strategy`
  activé pour un déploiement derrière un reverse proxy TLS.

## Lancement des tests

```bash
./mvnw verify
```

- `mvn test` (Surefire) exécute les tests unitaires purs (`*Test.java`, JUnit + Mockito, sans
  Spring ni Docker).
- `mvn verify` (Failsafe) exécute en plus les tests d'intégration (`*IT.java`) qui démarrent un
  vrai PostgreSQL via **Testcontainers** : migrations Flyway réelles, validation Hibernate
  (`ddl-auto=validate`), et scénarios bout en bout via `MockMvc` (cookies inclus).

Un Docker fonctionnel est requis pour les tests d'intégration.

## Accès au contrat OpenAPI

Avec `SWAGGER_ENABLED=true` (profil `dev`) :

- Swagger UI : `http://localhost:8080/swagger-ui.html`
- Document OpenAPI JSON : `http://localhost:8080/v3/api-docs`

Swagger est explicitement désactivé par défaut en production.

## Choix de sécurité

- **Aucun mot de passe pour les invités.** Un invité s'authentifie uniquement via le jeton opaque
  de son invitation (QR code), jamais via identifiant/mot de passe.
- **QR code sans donnée personnelle.** L'URL encodée dans le QR ne contient qu'un jeton aléatoire
  opaque (256 bits, encodé base64url) — ni nom, ni prénom, ni identifiant interne. Seul le hash
  SHA-256 du jeton est stocké en base ; le jeton en clair n'est retourné qu'une seule fois, dans la
  réponse de génération/régénération côté admin, et n'est jamais persisté.
- **Session opaque unique pour tous les acteurs.** Participants et membres de l'organisation
  partagent le même mécanisme : un jeton opaque aléatoire, dont seul le hash est stocké
  (`app_session`), livré via un cookie `HttpOnly`. `Secure` et `SameSite` sont pilotés par
  variables d'environnement (désactivé en dev sur HTTP local, obligatoire en prod). Cela permet à
  un invité de fermer l'application et de retrouver sa session, ses points et ses victoires.
- **Comptes de l'organisation avec mot de passe BCrypt.** `ADMIN`, `INTERVENANT`, `JURY` et
  `PROJECTION` sont des comptes réels (`staff_account`, mot de passe hashé BCrypt). Le premier
  compte admin se crée uniquement depuis des variables d'environnement (jamais commité).
- **Exclusions `HARD` immuables.** La suppression d'une exclusion `HARD` est refusée à double
  niveau : le contrôleur d'administration des exclusions n'est accessible qu'au rôle `ADMIN` (donc
  jamais à l'intervenant), et le service métier (`PairingExclusionService`) refuse la suppression
  d'une exclusion `HARD` pour **tout** appelant, y compris un futur appel interne côté admin. La
  contrainte "Jessika Dijoux ne peut jamais être associée à Sandrine Santin / Patrick Santin" est
  représentée uniquement par des enregistrements `PairingExclusion` référencés par UUID de
  participant — aucune comparaison de nom dans le code.
- **CORS piloté par variable d'environnement**, avec `allowCredentials=true` pour que le frontend
  puisse envoyer le cookie de session en cross-origin.
- **Erreurs JSON cohérentes** : chaque erreur retourne `{ code, message, status, path, timestamp,
  details[] }` (`ApiErrorResponse`), avec des messages en français.

## Fonctionnalités volontairement reportées

Hors périmètre de cette première phase (socle uniquement) :

- Jeux, questions, réponses, votes, équipes de personnages, classement des jeux.
- QR code général de secours (bris de glace).
- Synchronisation temps réel (WebSocket/SSE) du salon d'attente — la première version utilise des
  endpoints REST et un heartbeat simple ; les services sont conçus pour accueillir une couche
  temps réel plus tard sans réécriture.
- Algorithme de création des binômes — `PairingConstraintService` répond uniquement à "ces deux
  participants peuvent-ils être associés ?", sans générer de binômes.
- Endpoints d'administration de l'événement au-delà de la configuration publique en lecture
  (création/mise à jour du titre, du statut, de la configuration visuelle) : le premier événement
  est créé via bootstrap par variables d'environnement ; l'édition complète de l'événement viendra
  avec l'interface d'administration.
- Génération de l'image PNG du QR code côté backend : l'API expose l'URL + le jeton opaque ; le
  rendu visuel du QR code est laissé au frontend, conformément à la contrainte "aucun code
  frontend" de ce dépôt.
