# Guide d'intégration API — Frontend

Documentation à destination de l'équipe frontend pour consommer l'API `wedding-games-back`.
Toutes les routes sont préfixées par `/api/v1`. Le format d'échange est **JSON** partout,
sauf indication contraire (export CSV, planche d'invitations PDF).

## Sommaire

- [Authentification & session](#authentification--session)
- [Format d'erreur commun](#format-derreur-commun)
- [Enums](#enums)
- [1. Événement (public)](#1-événement-public)
- [2. Authentification staff](#2-authentification-staff)
- [3. Invitations (invité, public)](#3-invitations-invité-public)
- [4. Session courante (transverse)](#4-session-courante-transverse)
- [5. Salon d'attente — participant](#5-salon-dattente--participant)
- [6. Salon d'attente — staff (intervenant/admin)](#6-salon-dattente--staff-intervenantadmin)
- [7. Admin — Participants](#7-admin--participants)
- [8. Admin — Invitations](#8-admin--invitations)
- [9. Admin — Exclusions de matchmaking](#9-admin--exclusions-de-matchmaking)
- [10. Admin — Comptes staff](#10-admin--comptes-staff)
- [11. Admin — Configuration de l'événement](#11-admin--configuration-de-lévénement)
- [12. Admin — Personnages](#12-admin--personnages)
- [13. Matchmaking (équipes)](#13-matchmaking-équipes)
- [14. Équipe — participant](#14-équipe--participant)
- [15. Parties (jeux)](#15-parties-jeux)
- [16. Questions](#16-questions)
- [17. Quiz — réponse d'équipe en direct](#17-quiz--réponse-déquipe-en-direct)
- [18. Modération des réponses (quiz)](#18-modération-des-réponses-quiz)
- [19. Vote public](#19-vote-public)
- [20. Finalistes & décision du jury](#20-finalistes--décision-du-jury)
- [21. Scores & podium](#21-scores--podium)
- [22. Who Said It](#22-who-said-it)
- [23. Blind test](#23-blind-test)
- [24. Verrou de contrôle](#24-verrou-de-contrôle)
- [25. Projection](#25-projection)
- [26. Admin — Journal d'audit](#26-admin--journal-daudit)
- [Matrice des rôles](#matrice-des-rôles)
- [Exemple d'appel (fetch)](#exemple-dappel-fetch)
- [À noter pour le frontend](#à-noter-pour-le-frontend)

---

## Authentification & session

Il n'y a **pas de JWT ni de header `Authorization`**. L'authentification repose sur un
**cookie de session opaque, `HttpOnly`**, posé automatiquement par le navigateur.

- Nom du cookie : `wg_session` (configurable côté backend, mais c'est la valeur par défaut).
- Le cookie est posé par le backend via `Set-Cookie` lors de :
  - `POST /api/v1/invitations/{token}/confirm` ou `POST /api/v1/invitations/fallback/{code}/confirm` (invité),
  - `POST /api/v1/auth/staff/login` (staff).
- Il est effacé par `POST /api/v1/session/logout`.
- **Toutes les requêtes fetch/axios doivent envoyer les cookies** :
  - `fetch(url, { credentials: 'include' })`
  - `axios.create({ withCredentials: true })`
- Le backend est en **CORS avec `allowCredentials: true`** ; l'origine du frontend doit être
  déclarée côté backend (`APP_CORS_ALLOWED_ORIGINS`), sinon le navigateur bloquera la requête.
- Il n'y a rien à stocker côté frontend (pas de token en `localStorage`) : le cookie suffit.
  Après login/confirm, appeler `GET /api/v1/session/me` pour connaître l'identité/le rôle actif.
- Deux familles d'acteurs partagent le même mécanisme de session :
  - **`PARTICIPANT`** : un invité, authentifié uniquement via le jeton d'invitation (QR code) ou
    son code de secours, jamais par mot de passe.
  - **`STAFF`** : un membre de l'organisation (`ADMIN`, `INTERVENANT`, `JURY`, `PROJECTION`),
    authentifié par identifiant/mot de passe.

Codes HTTP transverses liés à l'auth :

| Cas | Code |
|---|---|
| Pas de cookie / cookie invalide ou expiré sur une route protégée | `401 Unauthorized` |
| Cookie valide mais rôle insuffisant pour la route | `403 Forbidden` (`ACCESS_DENIED`) |
| Identifiants staff invalides (`POST /auth/staff/login`) | `401 Unauthorized` (`INVALID_CREDENTIALS`) |

## Format d'erreur commun

Toute erreur (4xx/5xx) renvoie le même corps JSON :

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Participant introuvable.",
  "status": 404,
  "path": "/api/v1/admin/participants/...",
  "timestamp": "2026-08-18T10:00:00Z",
  "details": []
}
```

- `code` : identifiant technique stable, à utiliser pour réagir programmatiquement (ne jamais
  matcher sur `message`, qui est un texte français destiné à l'affichage/au debug).
- `details` : uniquement rempli pour les erreurs de validation (400), sous la forme
  `[{ "field": "firstName", "message": "..." }]`.

Codes rencontrés dans l'API, groupés par HTTP status :

**`400 Bad Request`**

| `code` | Où |
|---|---|
| `VALIDATION_ERROR` | Corps de requête invalide (`@Valid` échoué) — voir `details[]` |
| `SAME_PARTICIPANT` | Création d'exclusion avec `participantAId == participantBId` |
| `INVITATION_BATCH_EMPTY` | Génération en lot sans aucun participant à traiter |
| `LATECOMER_SAME_PARTICIPANT` | Tentative d'apparier un retardataire avec lui-même |
| `MATCHMAKING_NOT_ENOUGH_PARTICIPANTS` | Pas assez de participants présents pour former des équipes |
| `MATCHMAKING_NOT_ENOUGH_CHARACTERS` | Pas assez de personnages actifs pour assigner tout le monde |
| `PARTICIPANT_NOT_A_LATECOMER` | Le participant visé n'est pas (ou plus) un retardataire à intégrer |
| `IMPORT_FILE_EMPTY` | Fichier d'import CSV/Excel vide ou manquant |
| `IMPORT_FILE_UNREADABLE` | Fichier d'import illisible ou corrompu |
| `IMPORT_UNSUPPORTED_FORMAT` | Extension de fichier non supportée pour l'import |
| `CONTENT_TOO_LONG` | Question Who Said It dépassant la longueur max configurée |

**`401 Unauthorized`**

| `code` | Où |
|---|---|
| `INVALID_CREDENTIALS` | Login staff incorrect ou compte désactivé |

**`403 Forbidden`**

| `code` | Où |
|---|---|
| `ACCESS_DENIED` | Rôle insuffisant pour la route |

**`404 Not Found`**

| `code` | Où |
|---|---|
| `RESOURCE_NOT_FOUND` | Ressource (événement, participant, jeu, question, réponse, compte…) introuvable |
| `INVALID_INVITATION` | Jeton ou code de secours d'invitation inconnu ou révoqué |

**`409 Conflict`**

| `code` | Où |
|---|---|
| `EXCLUSION_ALREADY_EXISTS` | Exclusion déjà existante entre les deux participants |
| `USERNAME_TAKEN` | Nom d'utilisateur staff déjà pris |
| `HARD_EXCLUSION_IMMUTABLE` | Tentative de suppression d'une exclusion `HARD` (interdit, définitif) |
| `PARTICIPANT_HAS_HARD_EXCLUSION` | Suppression d'un participant impliqué dans une exclusion `HARD` |
| `CHARACTER_NAME_TAKEN` | Nom de personnage déjà utilisé sur l'événement |
| `CHARACTER_ASSIGNED_TO_TEAM` | Suppression d'un personnage déjà assigné à une équipe |
| `MATCHMAKING_INFEASIBLE` | Aucune répartition en équipes ne respecte les exclusions absolues |
| `LATECOMER_ALREADY_ON_A_TEAM` | Le retardataire a déjà une équipe |
| `LATECOMER_HARD_EXCLUSION` | Intégration refusée : exclusion absolue avec l'équipe/le binôme ciblé |
| `LATECOMER_TEAM_NOT_A_BINOME` | L'équipe ciblée n'est pas un binôme (déjà un trio) |
| `INVALID_LOBBY_TRANSITION` | Transition de statut de salon invalide (ex. fermer un salon déjà fermé) |
| `GAME_NOT_ACTIVE` | Action de pilotage refusée : la partie n'est pas active |
| `INVALID_GAME_STATUS_TRANSITION` | Transition de statut de partie invalide |
| `INVALID_GAME_PHASE_TRANSITION` | Transition de phase de partie invalide |
| `INVALID_QUESTION_STATUS_TRANSITION` | Transition de statut de question invalide (ex. activer une question déjà active) |
| `QUESTION_NOT_ACTIVE` | Réponse refusée : la question n'est pas active |
| `QUESTION_NOT_CLOSED` | Vote refusé : la question n'est pas fermée |
| `ANSWER_NOT_IN_CONTROL` | Modification refusée : ce participant n'a pas la main sur la réponse |
| `ANSWER_NOT_ACCEPTED` | Vote refusé : cette réponse n'a pas été acceptée en modération |
| `ANSWER_NOT_A_FINALIST` | Choix du jury refusé : cette réponse n'est pas dans le top des finalistes |
| `VOTE_ALREADY_CAST` | Ce participant a déjà voté pour cette question |
| `VOTE_SELF_TEAM_FORBIDDEN` | Tentative de voter pour la réponse de sa propre équipe |
| `JURY_DECISION_NOT_CHOSEN` | Confirmation refusée : aucune réponse n'a encore été choisie |
| `JURY_DECISION_ALREADY_CONFIRMED` | Modification refusée : la décision du jury est déjà confirmée (définitive) |
| `JURY_DECISION_NOT_CONFIRMED` | Bonus/révélation refusés : la décision n'est pas encore confirmée |
| `INVALID_TRACK_STATUS_TRANSITION` | Transition de statut de morceau invalide |
| `TRACK_NOT_ACTIVE` | Démarrage du chronomètre refusé : le morceau n'est pas actif |
| `GAME_CONTROL_LOCKED` | Prise de contrôle refusée : un autre intervenant pilote déjà la partie |
| `GAME_CONTROL_NOT_HELD_BY_YOU` | Relâchement refusé : ce n'est pas vous qui détenez le verrou (sauf `ADMIN`) |
| `INVALID_WHO_SAID_IT_QUESTION_TRANSITION` | Transition de statut de question Who Said It invalide |
| `WHO_SAID_IT_QUESTION_ALREADY_PLAYED` | Modification refusée : cette question a déjà été jouée |
| `WHO_SAID_IT_QUESTION_LIMIT_REACHED` | Le participant a déjà atteint son quota de questions proposées |
| `LOBBY_NOT_OPEN` | Proposition/modification de question refusée : le salon n'est plus ouvert |
| `NO_ACCEPTED_WHO_SAID_IT_QUESTION` | Tirage aléatoire refusé : aucune question acceptée disponible |

**`500 Internal Server Error`**

| `code` | Où |
|---|---|
| `INTERNAL_ERROR` | Erreur inattendue |

## Enums

```ts
type EventStatus = /* voir WeddingEvent — champ status renvoyé tel quel */ string;

type ParticipantType = 'GUEST' | 'SPOUSE' | 'ORGANIZER';
type ParticipantStatus = 'INVITED' | 'CONFIRMED' | 'CONNECTED' | 'PAUSED' | 'ABSENT';

type InvitationStatus = 'ACTIVE' | 'REVOKED';

type LobbyStatus = 'CLOSED' | 'OPEN' | 'LOCKED' | 'ACTIVE' | 'PAUSED' | 'FINISHED';
type LobbyConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'LATE' | 'READY';

type ExclusionType = 'HARD' | 'PREFERENCE'; // HARD = jamais contournable, PREFERENCE = indicatif
type Gender = 'MALE' | 'FEMALE'; // optionnel, sert uniquement à biaiser le matchmaking

type StaffRole = 'ADMIN' | 'INTERVENANT' | 'JURY' | 'PROJECTION';
// Role effectif de session (staff ou participant) :
type Role = 'ADMIN' | 'INTERVENANT' | 'JURY' | 'PARTICIPANT' | 'PROJECTION';
type ActorType = 'PARTICIPANT' | 'STAFF';

// --- Jeux ---
type GameType = 'QUIZ' | 'WHO_SAID_IT' | 'BLIND_TEST' | 'CUSTOM';
type GameStatus = 'DRAFT' | 'READY' | 'ACTIVE' | 'PAUSED' | 'FINISHED';
// Cycle partagé par tous les types de jeu; un type de jeu donné ne visite pas forcément
// toutes les phases (le blind test n'a pas de phase JURY, par exemple).
type GamePhase = 'LOBBY' | 'PREPARATION' | 'QUESTION' | 'ANSWERS_CLOSED' | 'VOTE' | 'JURY' | 'RESULT';

type QuestionStatus = 'PENDING' | 'ACTIVE' | 'CLOSED';
type QuestionSource = 'ADMIN' | 'GUEST'; // GUEST = proposée par un invité (Who Said It), modérée

type AnswerModerationStatus = 'PENDING' | 'ACCEPTED' | 'HIDDEN';

type JuryDecisionStatus = 'PENDING' | 'CHOSEN' | 'CONFIRMED';

type WhoSaidItQuestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'PLAYED';

type BlindTestVariant = 'SLOWED_DOWN' | 'REVERSED' | 'LYRICS_CONTINUATION';
type TrackStatus = 'PENDING' | 'ACTIVE' | 'CLOSED';

type ParticipantImportRowStatus = 'VALID' | 'DUPLICATE_IN_FILE' | 'DUPLICATE_EXISTING' | 'REJECTED';
```

---

## 1. Événement (public)

### `GET /api/v1/events/{slug}/public`

Configuration publique et non sensible d'un événement. **Aucune authentification requise.**
À appeler en premier pour afficher le thème/titre de la page d'accueil/invitation.

**Réponse `200`** — `EventPublicConfigResponse`

```json
{
  "id": "uuid",
  "slug": "seed-wedding",
  "title": "Notre mariage",
  "language": "fr-FR",
  "status": "...",
  "visualConfig": { "...": "objet libre, clé/valeur, pour le thème visuel" }
}
```

Erreurs : `404 RESOURCE_NOT_FOUND` si le slug n'existe pas.

---

## 2. Authentification staff

### `POST /api/v1/auth/staff/login`

Authentifie un membre de l'organisation et pose le cookie de session. **Public.**

**Corps de requête** — `StaffLoginRequest`

```json
{ "username": "string (requis)", "password": "string (requis)" }
```

**Réponse `200`** — `StaffAccountResponse` (voir [section 10](#10-admin--comptes-staff))
Le cookie `wg_session` est posé automatiquement par le navigateur (`Set-Cookie`).

Erreurs : `401 INVALID_CREDENTIALS` si identifiants incorrects ou compte désactivé
(`active: false`).

---

## 3. Invitations (invité, public)

Flux de l'invité scannant son QR code, ou saisissant son code de secours — **aucune
authentification préalable requise** pour ces routes.

### `GET /api/v1/invitations/{token}/resolve`

Résout le jeton opaque contenu dans l'URL du QR code en une identité à faire confirmer
("Bonjour, es-tu bien {firstName} ?"), **sans ouvrir de session**.

**Réponse `200`** — `InvitationResolveResponse`

```json
{
  "participantId": "uuid",
  "firstName": "Jessika",
  "displayName": "Jessika Dijoux",
  "eventSlug": "seed-wedding",
  "eventTitle": "Notre mariage"
}
```

Erreurs : `404 INVALID_INVITATION` si le jeton est inconnu ou révoqué.

### `POST /api/v1/invitations/{token}/confirm`

Confirme l'identité et **ouvre la session participante** (pose le cookie `wg_session`).
À appeler uniquement après que l'utilisateur a confirmé "oui, c'est bien moi" sur l'écran de
résolution.

**Réponse `200`** — `ParticipantSessionResponse` (voir plus bas)

Erreurs : `404 INVALID_INVITATION`.

### `GET /api/v1/invitations/fallback/{code}/resolve` et `POST /api/v1/invitations/fallback/{code}/confirm`

Strictement équivalentes aux deux routes ci-dessus, mais à partir du **code de secours à 6
caractères** plutôt que du jeton complet du QR code — pour le cas où l'invité a perdu ou ne peut
pas scanner son QR. Mêmes formes de réponse, mêmes erreurs.

---

## 4. Session courante (transverse)

Route partagée par les deux types d'acteurs, utile pour un bootstrap d'app (`useEffect` au
chargement) ou pour rafraîchir le score du participant.

### `GET /api/v1/session/me`

**Auth : cookie valide requis** (participant ou staff, `401` sinon).

**Réponse `200`** — `SessionMeResponse`

```json
{
  "actorType": "PARTICIPANT",
  "role": "PARTICIPANT",
  "participant": {
    "participantId": "uuid",
    "eventId": "uuid",
    "eventSlug": "seed-wedding",
    "firstName": "Jessika",
    "displayName": "Jessika Dijoux",
    "status": "CONNECTED",
    "totalPoints": 0,
    "totalWins": 0
  },
  "staff": null
}
```

ou, pour un acteur staff :

```json
{
  "actorType": "STAFF",
  "role": "ADMIN",
  "participant": null,
  "staff": {
    "id": "uuid",
    "username": "admin",
    "displayName": "Administrateur",
    "role": "ADMIN",
    "active": true,
    "createdAt": "2026-08-18T10:00:00Z"
  }
}
```

`participant` et `staff` sont **mutuellement exclusifs** (l'un des deux est `null` selon
`actorType`). Utiliser `actorType`/`role` pour aiguiller le routing frontend (écran invité,
back-office admin/intervenant, écran jury, écran projection).

### `POST /api/v1/session/logout`

**Auth : aucune vérification de rôle** (fonctionne même avec un cookie déjà expiré/absent,
idempotent). Révoque la session côté serveur et efface le cookie.

**Réponse `204 No Content`.**

---

## 5. Salon d'attente — participant

Base : `/api/v1/lobby` — **Auth : rôle `PARTICIPANT`.**

### `POST /heartbeat`

À appeler périodiquement (ex. toutes les 10-15s) tant que l'invité est sur l'écran de salon
d'attente, pour signaler sa présence. Aucun corps de requête ; l'identité vient du cookie de
session.

**Réponse `200`** — `LobbyHeartbeatResponse`

```json
{ "connectionStatus": "CONNECTED", "lastActivityAt": "2026-08-18T10:05:00Z" }
```

### `POST /ready`

Déclare le participant courant **prêt**, un statut de présence distinct d'une simple connexion
(`CONNECTED`). Aucun corps de requête.

**Réponse `200`** — `LobbyHeartbeatResponse` (`connectionStatus: "READY"`).

### `GET /`

État du salon adapté à ce que le participant a le droit de voir (pas de liste nominative — ça
reste réservé au staff).

**Réponse `200`** — `LobbyParticipantStatusResponse`

```json
{ "status": "OPEN", "presentCount": 42, "welcomeMessage": "Bienvenue !" }
```

---

## 6. Salon d'attente — staff (intervenant/admin)

Base : `/api/v1/staff/events/{eventId}/lobby`
**Auth : rôle `INTERVENANT` ou `ADMIN`.**

| Méthode | Route | Description | Réponse |
|---|---|---|---|
| `GET` | `/` | État courant du salon (statut, `openedAt`/`closedAt`) — pour le bandeau intervenant, sans avoir à déclencher de transition | `LobbyResponse` |
| `POST` | `/open` | Ouvre le salon (statut → `OPEN`) | `LobbyResponse` |
| `POST` | `/close` | Ferme le salon (statut → `CLOSED`) | `LobbyResponse` |
| `POST` | `/lock` | Verrouille le salon (statut → `LOCKED`, plus d'admission) | `LobbyResponse` |
| `POST` | `/start` | Démarre l'événement (statut → `ACTIVE`) | `LobbyResponse` |
| `POST` | `/pause` | Met l'événement en pause (statut → `PAUSED`) | `LobbyResponse` |
| `POST` | `/resume` | Reprend l'événement (statut → `ACTIVE`) | `LobbyResponse` |
| `POST` | `/finish` | Termine l'événement (statut → `FINISHED`) | `LobbyResponse` |
| `GET` | `/participants` | Liste tous les participants présents/inscrits dans le salon | `LobbyParticipantResponse[]` |
| `POST` | `/participants/{participantId}/late` | Marque un participant en retard (`connectionStatus: LATE`) | `LobbyParticipantResponse` |
| `POST` | `/participants/{participantId}/admit` | Admet/réadmet un participant (`connectionStatus: CONNECTED`) | `LobbyParticipantResponse` |

`LobbyResponse` :

```json
{ "id": "uuid", "eventId": "uuid", "status": "OPEN", "openedAt": "...", "closedAt": null }
```

`LobbyParticipantResponse` :

```json
{
  "participantId": "uuid",
  "displayName": "Jessika Dijoux",
  "connectionStatus": "CONNECTED",
  "arrivedAt": "2026-08-18T10:00:00Z",
  "lastActivityAt": "2026-08-18T10:05:00Z",
  "possibleDuplicate": false,
  "possibleQrReuse": false
}
```

> `possibleDuplicate`/`possibleQrReuse` sont des signaux **réservés au staff** (jamais renvoyés
> au participant lui-même, voir `LobbyHeartbeatResponse` ci-dessus) : à afficher comme un
> avertissement discret dans le back-office plutôt qu'à bloquer quoi que ce soit côté UI.

Note : le salon est créé implicitement (`getOrCreate`) au premier appel sur un événement — pas
besoin d'endpoint de création dédié. Idem pour `/participants/{id}/late` et `/admit` : si le
participant n'a pas encore d'entrée dans le salon, elle est créée à la volée.

Il existe aussi une variante **lecture seule pour l'admin**, sans le pilotage
ouverture/fermeture :

- `GET /api/v1/admin/events/{eventId}/lobby` (rôle `ADMIN`) → `LobbyResponse`
- `GET /api/v1/admin/events/{eventId}/lobby/participants` (rôle `ADMIN`) → `LobbyParticipantResponse[]`

Erreurs communes à toutes les transitions : `409 INVALID_LOBBY_TRANSITION`.

---

## 7. Admin — Participants

Base : `/api/v1/admin` — **Auth : rôle `ADMIN`.**

### `GET /events/{eventId}/participants`

Liste (et filtre) les participants d'un événement. Réponse : `ParticipantResponse[]`.
Query params optionnels : `status` (`ParticipantStatus`), `tableLabel`, `participantType`
(`ParticipantType`), `query` (recherche libre nom/prénom/nom affiché).

### `GET /events/{eventId}/participants/export`

Export CSV téléchargeable de la même liste filtrée (mêmes query params). Réponse :
`text/csv;charset=UTF-8`, en-tête `Content-Disposition: attachment; filename="participants.csv"`.
Colonnes : `prenom,nom,nom_affiche,table,type,statut,points,victoires`.

### `POST /events/{eventId}/participants`

Crée un participant. **Corps** — `ParticipantCreateRequest` :

```json
{
  "firstName": "string, requis, max 100",
  "lastName": "string, requis, max 100",
  "displayName": "string, requis, max 150",
  "tableLabel": "string, optionnel, max 50",
  "participantType": "GUEST | SPOUSE | ORGANIZER"
}
```

**Réponse `201`** — `ParticipantResponse`.

### `GET /participants/{id}`

**Réponse `200`** — `ParticipantResponse` :

```json
{
  "id": "uuid",
  "eventId": "uuid",
  "firstName": "Jessika",
  "lastName": "Dijoux",
  "displayName": "Jessika Dijoux",
  "tableLabel": "Table 3",
  "participantType": "GUEST",
  "status": "INVITED",
  "totalPoints": 0,
  "totalWins": 0,
  "createdAt": "...",
  "updatedAt": "..."
}
```

### `PUT /participants/{id}`

Remplace toutes les infos éditables. **Corps** — `ParticipantUpdateRequest` :

```json
{
  "firstName": "string, requis, max 100",
  "lastName": "string, requis, max 100",
  "displayName": "string, requis, max 150",
  "tableLabel": "string, optionnel, max 50",
  "participantType": "GUEST | SPOUSE | ORGANIZER",
  "status": "INVITED | CONFIRMED | CONNECTED | PAUSED | ABSENT"
}
```

**Réponse `200`** — `ParticipantResponse`. Note : c'est un `PUT` complet, pas un `PATCH` — tous
les champs doivent être envoyés (relire l'objet courant avant d'éditer si l'UI n'affiche qu'un
sous-ensemble de champs).

### `DELETE /participants/{id}`

**Réponse `204 No Content`.** Cette action est journalisée dans le
[journal d'audit](#26-admin--journal-daudit) (`PARTICIPANT_DELETED`).

Erreurs : `409 PARTICIPANT_HAS_HARD_EXCLUSION` si ce participant est impliqué dans une exclusion
`HARD` — le frontend doit gérer ce cas explicitement (ex. rediriger vers l'écran d'exclusions).

### `POST /participants/{id}/disable`

Désactive un participant (`status: DISABLED` — attention, distinct de `ABSENT`). **Réponse
`200`** — `ParticipantResponse`.

### `PATCH /participants/{id}/status`

**Corps** — `ParticipantStatusUpdateRequest` : `{ "status": "..." }`. **Réponse `200`** —
`ParticipantResponse`.

### `PATCH /participants/{id}/table`

**Corps** — `ParticipantTableUpdateRequest` : `{ "tableLabel": "..." }`. **Réponse `200`** —
`ParticipantResponse`.

### Import CSV/Excel — `POST /events/{eventId}/participants/import/preview` et `/confirm`

Base : `/api/v1/admin/events/{eventId}/participants/import` — **Auth : rôle `ADMIN`.**

Flux en deux temps : l'admin uploade un fichier, obtient une **prévisualisation** ligne par
ligne (valide / doublon / rejetée) sans rien créer, puis confirme explicitement les lignes à
importer.

#### `POST /preview` (`multipart/form-data`, champ `file`)

**Réponse `200`** — `ParticipantImportPreviewResponse` :

```json
{
  "rows": [
    {
      "rowNumber": 2,
      "firstName": "Alice",
      "lastName": "Wonderland",
      "displayName": "Alice Wonderland",
      "tableLabel": "Table 5",
      "participantType": "GUEST",
      "status": "VALID",
      "rejectionReason": null
    }
  ],
  "totalRows": 1,
  "validCount": 1,
  "duplicateCount": 0,
  "rejectedCount": 0
}
```

`status` (`ParticipantImportRowStatus`) : `VALID`, `DUPLICATE_IN_FILE` (doublon dans le fichier
lui-même), `DUPLICATE_EXISTING` (correspond à un participant déjà en base), ou `REJECTED`
(`rejectionReason` alors rempli).

Erreurs : `400 IMPORT_FILE_EMPTY`, `400 IMPORT_FILE_UNREADABLE`, `400 IMPORT_UNSUPPORTED_FORMAT`.

#### `POST /confirm`

**Corps** — `ParticipantImportConfirmRequest` : les lignes que l'admin a choisi de garder après
relecture de la prévisualisation (typiquement les `VALID`, mais le frontend peut laisser l'admin
décider ligne par ligne).

```json
{
  "rows": [
    { "firstName": "Alice", "lastName": "Wonderland", "displayName": "Alice Wonderland", "tableLabel": "Table 5", "participantType": "GUEST" }
  ]
}
```

**Réponse `200`** — `ParticipantImportConfirmResponse` : `{ "created": [ParticipantResponse, ...], "createdCount": 1 }`.

---

## 8. Admin — Invitations

**Auth : rôle `ADMIN`.**

### Invitation individuelle — base `/api/v1/admin/participants/{participantId}/invitation`

#### `POST /`

Génère une nouvelle invitation pour ce participant, **invalidant automatiquement l'ancien
jeton actif** s'il existait (une seule invitation active à la fois par participant).

**Réponse `201`** — `InvitationAdminResponse` :

```json
{
  "invitationId": "uuid",
  "participantId": "uuid",
  "rawToken": "chaîne opaque base64url",
  "invitationUrl": "http://.../invite/<rawToken>",
  "fallbackCode": "ABC234",
  "createdAt": "..."
}
```

> ⚠️ **`rawToken`/`invitationUrl` ne sont retournés qu'une seule fois, ici.** Le backend ne
> stocke que le hash SHA-256 du jeton et ne peut jamais le re-livrer. Si le frontend a besoin de
> régénérer un QR code, il faut rappeler cette route (ce qui révoquera l'ancien jeton) — pas de
> route "récupérer le jeton actuel en clair". `fallbackCode`, lui, reste consultable ensuite via
> `GET /`.
>
> C'est ce champ `invitationUrl` (ou `rawToken`) que le frontend encode dans le QR code affiché/
> imprimé pour l'invité. Le rendu visuel du QR code (génération de l'image) est entièrement à la
> charge du frontend.

#### `GET /`

Consulte le statut de l'invitation active du participant, **sans exposer le jeton brut**
(à utiliser pour afficher "invitation envoyée / jeton valide" dans le back-office).

**Réponse `200`** — `InvitationStatusResponse` :

```json
{ "invitationId": "uuid", "status": "ACTIVE", "fallbackCode": "ABC234", "createdAt": "..." }
```

Erreurs : `404 RESOURCE_NOT_FOUND` si aucune invitation active n'existe pour ce participant.

#### `POST /revoke`

Révoque le jeton actif **sans en émettre un nouveau** — utile pour un QR perdu ou compromis :
coupe l'accès immédiatement, sans réémission instantanée. **Réponse `204 No Content`.**

#### `POST /fallback-code/renew`

Renouvelle uniquement le code de secours à 6 caractères, sans toucher au QR actif — utile si
l'invité craint que son code ait été vu par quelqu'un d'autre. **Réponse `200`** —
`{ "fallbackCode": "XYZ789" }`.

### Génération en lot + planche d'impression — `POST /api/v1/admin/events/{eventId}/participants/invitations/batch`

Génère (ou régénère) une invitation pour tout ou partie des participants de l'événement, et
retourne **directement la planche d'impression PDF** des QR codes.

**Corps** (optionnel) — `InvitationBatchRequest` : `{ "participantIds": ["uuid", ...] }` ; `null`
ou tableau vide/absent = tous les participants de l'événement.

**Réponse `200`** — `application/pdf`, en-tête
`Content-Disposition: attachment; filename="invitations-qr.pdf"`.

> Les jetons bruts ne sont **jamais** retournés en JSON par cette route : ils n'existent que le
> temps de produire le PDF, puis disparaissent définitivement — pas de moyen de les récupérer
> après coup autrement qu'en régénérant.

Erreurs : `400 INVITATION_BATCH_EMPTY` si la liste résolue de participants est vide. Cette
action est journalisée dans le [journal d'audit](#26-admin--journal-daudit)
(`INVITATION_BATCH_REGENERATED`, une seule entrée pour tout le lot).

---

## 9. Admin — Exclusions de matchmaking

**Auth : rôle `ADMIN` uniquement** (jamais accessible à `INTERVENANT`).

### `GET /api/v1/admin/events/{eventId}/exclusions`

Liste les exclusions de l'événement. Réponse : `PairingExclusionResponse[]`.

### `POST /api/v1/admin/events/{eventId}/exclusions`

**Corps** — `PairingExclusionCreateRequest` :

```json
{
  "participantAId": "uuid, requis",
  "participantBId": "uuid, requis (différent de participantAId)",
  "reason": "string, optionnel, max 300",
  "exclusionType": "HARD | PREFERENCE"
}
```

**Réponse `201`** — `PairingExclusionResponse` :

```json
{
  "id": "uuid",
  "eventId": "uuid",
  "participantAId": "uuid",
  "participantBId": "uuid",
  "reason": "string ou null",
  "exclusionType": "HARD",
  "locked": true,
  "createdAt": "..."
}
```

> Note : `participantAId`/`participantBId` dans la réponse sont normalisés (paire triée par
> UUID), donc pas nécessairement dans le même ordre que la requête d'origine.

Erreurs :
- `400 SAME_PARTICIPANT` si les deux IDs sont identiques.
- `404 RESOURCE_NOT_FOUND` si événement ou participant introuvable.
- `409 EXCLUSION_ALREADY_EXISTS` si la paire existe déjà.

Une exclusion `HARD` créée ici est journalisée dans le
[journal d'audit](#26-admin--journal-daudit) (`HARD_EXCLUSION_CREATED`) ; une `PREFERENCE` ne
l'est pas.

### `GET /api/v1/admin/exclusions/{id}`

**Réponse `200`** — `PairingExclusionResponse`.

### `PATCH /api/v1/admin/exclusions/{id}`

Met à jour uniquement le motif (`reason`). **Corps** — `PairingExclusionReasonUpdateRequest` :

```json
{ "reason": "string, optionnel, max 300" }
```

**Réponse `200`** — `PairingExclusionResponse`. Sur une exclusion `HARD`, journalisé
(`HARD_EXCLUSION_REASON_UPDATED`).

### `DELETE /api/v1/admin/exclusions/{id}`

**Réponse `204 No Content`.**

Erreurs : `409 HARD_EXCLUSION_IMMUTABLE` — **une exclusion `HARD` ne peut jamais être supprimée
via l'API**, quel que soit l'appelant. Le frontend doit désactiver/masquer le bouton de
suppression pour les lignes où `exclusionType === 'HARD'` (ou `locked === true`) plutôt que de
laisser l'utilisateur cliquer pour se prendre un 409.

### `GET /api/v1/admin/events/{eventId}/exclusions/check?participantAId=...&participantBId=...`

Vérifie à la volée si deux participants peuvent être appariés — utile pour un aperçu en direct
dans l'UI de constitution manuelle d'équipes, avant de lancer le matchmaking automatique.

**Réponse `200`** — `PairingCheckResponse` : `{ "canPair": true, "hasHardExclusion": false }`.

---

## 10. Admin — Comptes staff

Base : `/api/v1/admin/staff` — **Auth : rôle `ADMIN`.**

`StaffAccountResponse` (forme commune à toutes les réponses de cette section, et à
`session/me`/`auth/staff/login`) :

```json
{
  "id": "uuid",
  "username": "string",
  "displayName": "string",
  "role": "ADMIN | INTERVENANT | JURY | PROJECTION",
  "active": true,
  "createdAt": "..."
}
```

Le mot de passe (hash BCrypt) n'est **jamais** renvoyé par l'API.

> Les quatre rôles sont désormais tous actifs côté API : `JURY` pilote les endpoints de la
> [section 20](#20-finalistes--décision-du-jury), `PROJECTION` consulte l'écran agrégé de la
> [section 25](#25-projection) (et le podium/les morceaux actifs en lecture). Voir la
> [matrice des rôles](#matrice-des-rôles) pour le détail complet.

### `GET /`

Liste tous les comptes. Réponse : `StaffAccountResponse[]`.

### `POST /`

**Corps** — `StaffAccountCreateRequest` :

```json
{
  "username": "string, requis, max 100",
  "password": "string, requis, 8 à 200 caractères",
  "displayName": "string, requis, max 150",
  "role": "ADMIN | INTERVENANT | JURY | PROJECTION"
}
```

**Réponse `201`** — `StaffAccountResponse`.
Erreurs : `409 USERNAME_TAKEN` si le nom d'utilisateur existe déjà.

### `GET /{id}`

**Réponse `200`** — `StaffAccountResponse`.

### `PUT /{id}`

**Corps** — `StaffAccountUpdateRequest` :

```json
{
  "displayName": "string, requis, max 150",
  "role": "ADMIN | INTERVENANT | JURY | PROJECTION",
  "active": true,
  "password": "string, optionnel, 8 à 200 caractères — laisser vide/absent pour ne pas changer le mot de passe"
}
```

**Réponse `200`** — `StaffAccountResponse`. Mettre `active: false` désactive le compte
(login refusé ensuite, `401 INVALID_CREDENTIALS`), sans le supprimer.

### `DELETE /{id}`

**Réponse `204 No Content`.**

---

## 11. Admin — Configuration de l'événement

Base : `/api/v1/admin` — **Auth : rôle `ADMIN`.** Configuration éditable de l'événement
(thème visuel, textes, informations des mariés) — distincte de la lecture publique de la
[section 1](#1-événement-public), qui expose le même contenu en lecture seule et sans auth.

### `GET /event` et `PUT /event`

Raccourcis pratiques pour un déploiement **mono-événement** : agissent sur l'unique événement de
ce déploiement, sans avoir besoin de connaître son `eventId`.

### `GET /events/{eventId}` et `PUT /events/{eventId}`

Équivalents explicites par `eventId`, pour un usage multi-événements futur.

**Corps de `PUT`** — `EventConfigUpdateRequest` :

```json
{
  "title": "string, requis, max 200",
  "spouseOneName": "string, optionnel, max 150",
  "spouseTwoName": "string, optionnel, max 150",
  "eventDate": "2026-12-05",
  "venueName": "string, optionnel, max 200",
  "welcomeMessage": "string, optionnel, max 2000",
  "visualConfig": { "...": "objet libre, clé/valeur, pour le thème visuel" }
}
```

**Réponse `200`** — `EventPublicConfigResponse` (même forme que la [section 1](#1-événement-public)).
Tous les champs sauf `title` sont optionnels : un événement peut être créé puis configuré
progressivement.

---

## 12. Admin — Personnages

Base : `/api/v1/admin` — **Auth : rôle `ADMIN`.** Catalogue de personnages assignés aux
participants par le matchmaking (ex. couples de dessins animés, duos historiques…).

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/events/{eventId}/characters` | Liste les personnages de l'événement |
| `POST` | `/events/{eventId}/characters` | Crée un personnage (`201`) |
| `GET` | `/characters/{id}` | Consulte un personnage |
| `PUT` | `/characters/{id}` | Remplace un personnage |
| `POST` | `/characters/{id}/activate` | Rend le personnage disponible pour le matchmaking |
| `POST` | `/characters/{id}/deactivate` | Le retire du pool sans le supprimer |
| `DELETE` | `/characters/{id}` | Supprime le personnage (`204`) |

**Corps** (create/update) — `GameCharacterCreateRequest` / `GameCharacterUpdateRequest` (mêmes
champs) :

```json
{
  "name": "string, requis, max 100",
  "description": "string, optionnel, max 500",
  "avatarUrl": "string, optionnel, max 500",
  "gender": "MALE | FEMALE | null"
}
```

**Réponse** — `GameCharacterResponse` :

```json
{
  "id": "uuid",
  "eventId": "uuid",
  "name": "Sangoku",
  "description": "...",
  "avatarUrl": "https://...",
  "active": true,
  "gender": "MALE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

Erreurs : `409 CHARACTER_NAME_TAKEN` (nom déjà utilisé sur l'événement),
`409 CHARACTER_ASSIGNED_TO_TEAM` (suppression d'un personnage déjà assigné — le désactiver plutôt
que le supprimer si le matchmaking a déjà tourné).

Seuls les personnages `active: true` sont éligibles au tirage du matchmaking (section suivante).

---

## 13. Matchmaking (équipes)

**Auth : rôle `INTERVENANT` ou `ADMIN`.** Constitue les équipes (binômes, ou trios pour les
retardataires) à partir des participants présents dans le salon, en respectant les exclusions
absolues (`HARD`) et en essayant de respecter les préférences (`PREFERENCE`) et le genre des
personnages quand c'est possible.

### Base `/api/v1/staff/events/{eventId}/matchmaking`

#### `POST /launch`

Génère les équipes. **Peut être relancé à volonté** : les équipes précédentes sont remplacées à
chaque appel — pas de fusion incrémentale.

**Réponse `200`** — `TeamResponse[]` :

```json
[{
  "id": "uuid",
  "label": "Binôme 1",
  "members": [
    { "participantId": "uuid", "displayName": "Jessika Dijoux", "characterId": "uuid", "characterName": "Sangoku" }
  ]
}]
```

Erreurs :
- `400 MATCHMAKING_NOT_ENOUGH_PARTICIPANTS` / `400 MATCHMAKING_NOT_ENOUGH_CHARACTERS`.
- `409 MATCHMAKING_INFEASIBLE` si aucune répartition ne respecte les exclusions `HARD` (assez de
  monde et de personnages, mais la contrainte est insoluble).

#### `GET /teams`

Consulte les équipes actuelles sans relancer le matchmaking. **Réponse `200`** — `TeamResponse[]`.

### Retardataires — base `/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}`

Pour intégrer manuellement un participant arrivé après le lancement du matchmaking.

#### `GET /options`

**Réponse `200`** — `LatecomerOptionsResponse` :

```json
{
  "compatibleTeams": [ /* TeamResponse[], deviendraient un trio */ ],
  "compatibleLatecomers": [ { "participantId": "uuid", "displayName": "Bob Builder" } ]
}
```

Les deux listes sont **déjà filtrées** pour les exclusions `HARD` : tout ce qui est listé est
sûr à proposer dans l'UI.

#### `POST /join-team/{teamId}`

Intègre le retardataire dans un binôme existant, qui devient un trio. **Réponse `200`** —
`TeamResponse`.

#### `POST /pair-with/{otherParticipantId}`

Forme un nouveau binôme entre deux retardataires. **Réponse `200`** — `TeamResponse`.

Erreurs communes aux deux routes d'intégration :
`400 PARTICIPANT_NOT_A_LATECOMER`, `400 LATECOMER_SAME_PARTICIPANT`,
`409 LATECOMER_ALREADY_ON_A_TEAM`, `409 LATECOMER_HARD_EXCLUSION`,
`409 LATECOMER_TEAM_NOT_A_BINOME` (uniquement pour `/join-team`).

---

## 14. Équipe — participant

### `GET /api/v1/team/me`

**Auth : rôle `PARTICIPANT`.** Une fois le matchmaking effectué : mon personnage assigné et
celui de mon/mes partenaire(s) de binôme/trio.

**Réponse `200`** — `MyTeamResponse` :

```json
{
  "teamId": "uuid",
  "myCharacterId": "uuid",
  "myCharacterName": "Sangoku",
  "myCharacterAvatarUrl": "https://...",
  "myCharacterDescription": "...",
  "partners": [
    { "participantId": "uuid", "displayName": "Bob Builder", "characterId": "uuid", "characterName": "Krillin", "characterAvatarUrl": "https://..." }
  ]
}
```

---

## 15. Parties (jeux)

Une **partie** (`Game`) est une instance configurée d'un type de jeu (`QUIZ`, `WHO_SAID_IT`,
`BLIND_TEST`, `CUSTOM`) attachée à un événement, avec son propre cycle
`GameStatus`/`GamePhase` (voir [Enums](#enums)).

### Admin — configuration, base `/api/v1/admin` — **rôle `ADMIN`**

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/events/{eventId}/games` | Liste les parties de l'événement |
| `POST` | `/events/{eventId}/games` | Crée une partie (`201`) |
| `GET` | `/games/{id}` | Consulte une partie |

**Corps de création** — `GameCreateRequest` :

```json
{ "type": "QUIZ | WHO_SAID_IT | BLIND_TEST | CUSTOM", "title": "string, requis, max 200", "sequence": 0 }
```

### Intervenant — pilotage, base `/api/v1/staff` — **rôle `INTERVENANT` ou `ADMIN`**

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/events/{eventId}/games` | Liste les parties (pour choisir laquelle piloter) |
| `GET` | `/games/{gameId}` | Consulte une partie |
| `POST` | `/games/{gameId}/start` | Démarre (`ACTIVE`, phase `PREPARATION`) |
| `POST` | `/games/{gameId}/pause` | Met en pause (phase inchangée) |
| `POST` | `/games/{gameId}/resume` | Reprend une partie en pause |
| `POST` | `/games/{gameId}/next-question` | Passe à la question suivante (phase `QUESTION`) |

**Réponse (toutes les routes)** — `GameResponse` :

```json
{ "id": "uuid", "eventId": "uuid", "type": "QUIZ", "title": "Quiz absurde", "sequence": 0, "status": "ACTIVE", "phase": "QUESTION" }
```

Erreurs : `409 GAME_NOT_ACTIVE`, `409 INVALID_GAME_STATUS_TRANSITION`,
`409 INVALID_GAME_PHASE_TRANSITION`.

> Avant de piloter une partie, un intervenant doit généralement en avoir la main exclusive — voir
> [section 24, Verrou de contrôle](#24-verrou-de-contrôle).

### Participant — jeu en cours

#### `GET /api/v1/games/current` — **rôle `PARTICIPANT`**

Ce que la salle joue **maintenant**, pour l'écran de l'invité. C'est le seul endpoint qui expose
au participant l'`id` de la question active : à poller (ex. toutes les 3-5s) pour savoir quand
basculer vers l'écran de réponse (`/quiz/questions/{questionId}/answer`, question `ACTIVE`) ou de
vote (`/vote/questions/{questionId}/...`, question `CLOSED`).

L'événement est déduit de la session — aucun paramètre. **Jamais `404`** : quand rien n'est
lancé, les deux champs valent `null`.

**Réponse `200`** — `CurrentGameResponse` :

```json
{
  "game": { "id": "uuid", "eventId": "uuid", "type": "QUIZ", "title": "Quiz absurde", "sequence": 0, "status": "ACTIVE", "phase": "QUESTION" },
  "question": { "id": "uuid", "sequence": 0, "status": "ACTIVE", "prompt": "Quel est le comble pour un électricien ?" }
}
```

- `game` est `null` tant qu'aucune partie n'est `ACTIVE`/`PAUSED` ;
- `question` est `null` tant que la partie active n'a pas encore activé de question (phase
  `PREPARATION`). `question.status` : `ACTIVE` = réponse en cours, `CLOSED` = vote ouvert.

---

## 16. Questions

Une question appartient à une partie. Elle peut être préparée par l'admin (`source: ADMIN`) ou
provenir d'une proposition d'invité modérée (`source: GUEST`, réservé au jeu Who Said It — voir
[section 22](#22-who-said-it)).

### Admin, base `/api/v1/admin` — **rôle `ADMIN`**

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/games/{gameId}/questions` | Liste les questions d'une partie |
| `POST` | `/games/{gameId}/questions` | Crée une question (`201`) |
| `GET` | `/questions/{id}` | Consulte une question |

**Corps de création** — `QuestionCreateRequest` : `{ "prompt": "string, requis, max 1000", "sequence": 0 }`.

### Intervenant, base `/api/v1/staff` — **rôle `INTERVENANT` ou `ADMIN`**

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/games/{gameId}/questions` | Liste les questions (pour choisir laquelle piloter) |
| `GET` | `/questions/{questionId}` | Consulte une question |
| `POST` | `/questions/{questionId}/activate` | Active (`PENDING → ACTIVE`) : les équipes peuvent répondre |
| `POST` | `/questions/{questionId}/close` | Ferme (`ACTIVE → CLOSED`) : plus aucune réponse acceptée |

**Réponse (toutes les routes)** — `QuestionResponse` :

```json
{ "id": "uuid", "gameId": "uuid", "prompt": "...", "sequence": 0, "status": "ACTIVE", "source": "ADMIN" }
```

Erreurs : `409 INVALID_QUESTION_STATUS_TRANSITION`.

---

## 17. Quiz — réponse d'équipe en direct

Base : `/api/v1/quiz/questions/{questionId}/answer` — **Auth : rôle `PARTICIPANT`.**

**Une réponse par équipe et par question**, éditable en direct par le membre qui a "la main"
("prise de contrôle"). Reprendre la main transfère simplement le contrôle à celui qui la
redemande, en conservant le contenu déjà saisi.

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/` | État actuel de la réponse de mon équipe (à sonder/poller pour suivre la saisie en direct) |
| `POST` | `/take-control` | Je prends la main (ou la reprends à un coéquipier) |
| `PUT` | `/` | Met à jour le contenu — réservé à celui qui a la main |

**Corps de `PUT`** — `QuizAnswerUpdateRequest` : `{ "content": "string, requis, max 1000" }`.

**Réponse** — `QuizAnswerResponse` :

```json
{
  "questionId": "uuid",
  "teamId": "uuid",
  "content": "42",
  "controllingParticipantId": "uuid",
  "controllingParticipantName": "Jessika Dijoux",
  "lastEditedAt": "..."
}
```

Erreurs : `404 RESOURCE_NOT_FOUND` (aucune réponse commencée pour `GET`),
`409 QUESTION_NOT_ACTIVE`, `409 ANSWER_NOT_IN_CONTROL` (un autre membre de l'équipe a la main).

---

## 18. Modération des réponses (quiz)

**Auth : rôle `INTERVENANT` ou `ADMIN`.** Avant qu'une réponse d'équipe ne puisse être projetée,
soumise au vote ou transmise au jury, elle doit être modérée.

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/v1/staff/questions/{questionId}/answers` | Liste toutes les réponses (y compris masquées) |
| `POST` | `/api/v1/staff/answers/{answerId}/accept` | Accepte : éligible projection/vote/jury |
| `POST` | `/api/v1/staff/answers/{answerId}/hide` | Masque ou refuse : jamais projetée ni transmise |
| `PUT` | `/api/v1/staff/answers/{answerId}/content` | Corrige le contenu (typo) sans changer le sens |
| `POST` | `/api/v1/staff/questions/{questionId}/teams/{teamId}/relaunch` | Relance une équipe : réponse remise à vide, plus personne n'a la main |

**Corps de `PUT .../content`** — `AnswerCorrectionRequest` : `{ "content": "string, requis, max 1000" }`.

**Réponse** — `AnswerModerationResponse` :

```json
{
  "id": "uuid",
  "questionId": "uuid",
  "teamId": "uuid",
  "teamLabel": "Binôme 1",
  "content": "42",
  "moderationStatus": "PENDING | ACCEPTED | HIDDEN",
  "controllingParticipantId": "uuid ou null",
  "lastEditedAt": "..."
}
```

---

## 19. Vote public

Base : `/api/v1/vote/questions/{questionId}` — **Auth : rôle `PARTICIPANT`.** Le vote s'ouvre
une fois la question **fermée** (réponses figées, voir [section 16](#16-questions)).

### `GET /options`

Options de vote : réponses **acceptées uniquement**, en **ordre aléatoire** à chaque appel,
**jamais la réponse de sa propre équipe**, sans nom d'équipe ni personnage visible (anonymisé).

**Réponse `200`** — `VotingOptionResponse[]` : `[{ "answerId": "uuid", "content": "42" }]`.

### `POST /`

Vote pour une réponse — jamais celle de sa propre équipe, une seule fois par question.

**Corps** — `VoteCastRequest` : `{ "answerId": "uuid, requis" }`.

**Réponse `201`** — `VoteResponse` : `{ "id": "uuid", "questionId": "uuid", "answerId": "uuid" }`.

Erreurs : `409 QUESTION_NOT_CLOSED`, `409 ANSWER_NOT_ACCEPTED`,
`409 VOTE_SELF_TEAM_FORBIDDEN`, `409 VOTE_ALREADY_CAST`.

---

## 20. Finalistes & décision du jury

### Finalistes — `GET /api/v1/staff/questions/{questionId}/finalists`

**Auth : rôle `JURY`, `INTERVENANT` ou `ADMIN`.** Top 3 des réponses les plus votées — **par
paliers de vote distincts** : en cas d'égalité sur le palier qui serait la coupure, toutes les
réponses ex æquo sont conservées (jamais de tirage au sort qui éliminerait une égalité), donc
potentiellement plus de 3 réponses renvoyées.

Query param `revealVoteCount` (défaut `false`) : le nombre de votes est **masqué par défaut**
pour ne pas influencer le jugement du jury.

**Réponse `200`** — `FinalistResponse[]` :
`[{ "answerId": "uuid", "content": "42", "voteCount": null }]` (`voteCount` rempli seulement si
`revealVoteCount=true`).

### Décision du jury — base `/api/v1/staff/questions/{questionId}/jury-decision`

Machine à états : `PENDING → CHOSEN → CONFIRMED`, puis révélation optionnelle.

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| `GET` | `/` | `JURY`, `INTERVENANT`, `ADMIN` | Consulte l'état de la décision |
| `POST` | `/choose` | `JURY`, `ADMIN` | Choisit (ou change, tant que non confirmé) la réponse gagnante parmi les finalistes |
| `POST` | `/confirm` | `JURY`, `ADMIN` | Confirme (définitif) et attribue les points de la manche |
| `POST` | `/bonus` | `JURY`, `ADMIN` | Attribue un bonus optionnel supplémentaire |
| `POST` | `/reveal` | `JURY`, `ADMIN` | Révèle l'équipe gagnante |

**Corps de `/choose`** — `JuryChooseRequest` : `{ "answerId": "uuid, requis" }`.

**Corps de `/confirm` et `/bonus`** — `JuryPointsRequest` : `{ "points": 10, "reason": "string, optionnel" }`
(le barème est fourni par l'appelant, pas par le backend).

**Réponse** — `JuryDecisionResponse` :

```json
{ "questionId": "uuid", "chosenAnswerId": "uuid ou null", "chosenTeamId": "uuid ou null", "status": "CHOSEN", "revealed": false }
```

Erreurs : `409 ANSWER_NOT_A_FINALIST` (réponse choisie hors du top des finalistes),
`409 JURY_DECISION_NOT_CHOSEN` (confirmer sans avoir choisi),
`409 JURY_DECISION_ALREADY_CONFIRMED` (re-choisir après confirmation),
`409 JURY_DECISION_NOT_CONFIRMED` (bonus/révélation avant confirmation).

`/confirm` déclenche l'attribution de points — voir [section 21](#21-scores--podium) pour le
journal des scores qui en résulte.

---

## 21. Scores & podium

Base : `/api/v1/staff/events/{eventId}` — **Auth : rôle `INTERVENANT` ou `ADMIN`** (`GET /podium`
aussi ouvert à `JURY` et `PROJECTION`).

### `POST /scores`

Attribue (ou retire, avec un `points` négatif) des points à une équipe. **Le barème est fourni
par l'appelant** — le backend ne connaît aucune règle de points fixe.

**Corps** — `ScoreAwardRequest` :

```json
{ "gameId": "uuid, optionnel", "teamId": "uuid, requis", "points": 10, "reason": "string, optionnel, max 200" }
```

**Réponse `201`** — `ScoreResponse` :

```json
{ "id": "uuid", "eventId": "uuid", "gameId": "uuid ou null", "teamId": "uuid", "points": 10, "reason": "...", "createdAt": "..." }
```

### `GET /scores`

Liste le journal complet des points attribués pour l'événement (chronologique). Réponse :
`ScoreResponse[]`.

### `GET /podium`

Classement des équipes par total de points. **Égalités partagées** (même `rank`), jamais de
tirage au sort. **Réponse `200`** — `PodiumEntryResponse[]` :

```json
[{ "teamId": "uuid", "teamLabel": "Binôme 1", "totalPoints": 30, "rank": 1 }]
```

---

## 22. Who Said It

Jeu où les invités proposent eux-mêmes des questions ("Qui a dit... ?"), modérées par le staff,
puis tirées au hasard pour être jouées. (Anciennement nommé "Lui ou Elle" côté backend — les
routes ci-dessous sont les routes actuelles, en anglais.)

### Proposition — base `/api/v1/who-said-it/questions` — **rôle `PARTICIPANT`**

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/me` | Liste mes questions proposées |
| `POST` | `/` | Propose une nouvelle question |
| `PUT` | `/{id}` | Modifie une de mes questions, tant que le salon reste ouvert |

**Corps** (create/update) — `WhoSaidItQuestionRequest` :
`{ "content": "string, requis, max 280 par défaut", "revealAuthorConsent": true }`.

**Réponse** — `WhoSaidItQuestionResponse` :

```json
{
  "id": "uuid",
  "eventId": "uuid",
  "authorId": "uuid",
  "authorDisplayName": "Jessika Dijoux ou null",
  "content": "...",
  "status": "PENDING",
  "revealAuthorConsent": true,
  "createdAt": "...",
  "updatedAt": "..."
}
```

> `authorDisplayName` n'est rempli, pour un invité, que si `revealAuthorConsent` est `true` sur
> cette question. Le staff, lui, voit toujours `authorId` (traçabilité de modération) — voir
> plus bas.

Erreurs : `409 WHO_SAID_IT_QUESTION_LIMIT_REACHED` (quota de 2 questions par participant par
défaut, configurable), `400 CONTENT_TOO_LONG`, `409 LOBBY_NOT_OPEN` (le salon n'accepte plus de
propositions), `409 WHO_SAID_IT_QUESTION_ALREADY_PLAYED` (modification d'une question déjà
jouée, refusée).

### Modération — base `/api/v1/staff` — **rôle `INTERVENANT` ou `ADMIN`**

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/events/{eventId}/who-said-it/questions` | Liste toutes les questions proposées, pour modération |
| `POST` | `/who-said-it/questions/{id}/accept` | Accepte : éligible à la sélection aléatoire |
| `POST` | `/who-said-it/questions/{id}/reject` | Refuse : jamais sélectionnée |
| `PUT` | `/who-said-it/questions/{id}/content` | Corrige le contenu (typo) sans changer le sens |

**Corps de `PUT .../content`** — `WhoSaidItCorrectionRequest` : `{ "content": "string, requis, max 500" }`.

**Réponse** — `WhoSaidItQuestionResponse`, mais **côté staff `authorDisplayName` est toujours
rempli**, indépendamment du consentement (le staff voit qui a proposé quoi pour la modération —
seul le jeu en cours respecte le consentement, voir ci-dessous).

### Sélection — `POST /api/v1/staff/events/{eventId}/who-said-it/questions/select-random`

**Auth : rôle `INTERVENANT` ou `ADMIN`.** Tire au hasard une question **acceptée** et la fait
passer à `PLAYED` (statut terminal, jamais rejoué).

**Réponse `200`** — `WhoSaidItQuestionResponse` — **ici, `authorDisplayName` respecte le
consentement de l'auteur** même pour le staff : c'est le moment où ce consentement compte
vraiment, contrairement à la modération où le staff voit toujours qui a proposé quoi.

Erreurs : `409 NO_ACCEPTED_WHO_SAID_IT_QUESTION` si aucune question acceptée n'est disponible.

---

## 23. Blind test

Jeu de reconnaissance musicale en manches (`Track`), chacune dans une variante donnée.

### Catalogue admin — base `/api/v1/admin` — **rôle `ADMIN`**

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/games/{gameId}/tracks` | Liste les morceaux d'une partie |
| `POST` | `/games/{gameId}/tracks` | Crée un morceau (`201`) |
| `GET` | `/tracks/{id}` | Consulte un morceau |
| `PUT` | `/tracks/{id}` | Remplace un morceau |
| `DELETE` | `/tracks/{id}` | Supprime un morceau (`204`) |

**Corps** (create/update) — `TrackCreateRequest` / `TrackUpdateRequest` :

```json
{ "title": "string, requis, max 200", "artist": "string, requis, max 200", "variant": "SLOWED_DOWN | REVERSED | LYRICS_CONTINUATION", "sequence": 0 }
```

**Réponse** — `TrackResponse` : `{ "id": "uuid", "gameId": "uuid", "title": "...", "artist": "...", "variant": "REVERSED", "sequence": 0 }`.

### Format de manche — base `/api/v1/admin/games/{gameId}/blind-test-format` — **rôle `ADMIN`**

Config du format, créée à la volée au premier appel `GET` s'il n'existe pas encore.

- `GET /` → `BlindTestFormatResponse`
- `PUT /` (corps `BlindTestFormatRequest`) → `BlindTestFormatResponse`

```json
{ "roundDurationSeconds": 30, "pointsPerCorrectGuess": 10 }
```

(`roundDurationSeconds` ≥ 1, `pointsPerCorrectGuess` ≥ 0.) `BlindTestFormatResponse` ajoute
`gameId` en tête.

### Pilotage — base `/api/v1/staff` — **rôle `INTERVENANT` ou `ADMIN`** (les `GET` aussi ouverts à `PROJECTION`)

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/games/{gameId}/tracks/active` | Le morceau actuellement actif (et son chrono) |
| `GET` | `/tracks/{trackId}/state` | État (statut, chrono) d'un morceau |
| `POST` | `/tracks/{trackId}/activate` | Active (`PENDING → ACTIVE`) : devient la manche en cours |
| `POST` | `/tracks/{trackId}/close` | Ferme (`ACTIVE → CLOSED`) : manche terminée |
| `POST` | `/tracks/{trackId}/start-timer` | Lance le chrono, pour la durée configurée dans le format |

**Réponse** — `TrackStateResponse` (mêmes champs que `TrackResponse` + `status` + `remainingSeconds`) :

```json
{ "id": "uuid", "gameId": "uuid", "title": "...", "artist": "...", "variant": "REVERSED", "sequence": 0, "status": "ACTIVE", "remainingSeconds": 18 }
```

`remainingSeconds` est `null` tant que le chrono n'a pas été démarré, puis décompte en temps réel
côté serveur à chaque appel (pas de websocket : à sonder/poller côté frontend).

Erreurs : `404 RESOURCE_NOT_FOUND` (`GET .../active` si aucun morceau actif),
`409 INVALID_TRACK_STATUS_TRANSITION`, `409 TRACK_NOT_ACTIVE` (démarrage du chrono hors morceau actif).

---

## 24. Verrou de contrôle

Base : `/api/v1/staff/games/{gameId}/control-lock` — **Auth : rôle `INTERVENANT` ou `ADMIN`.**
Garantit qu'**un seul intervenant pilote une partie à la fois** (démarrage, questions, morceaux…).

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/` | Consulte qui pilote actuellement cette partie, le cas échéant |
| `POST` | `/claim` | Prend le contrôle de la partie |
| `POST` | `/release` | Relâche le contrôle |

**Réponse** — `GameControlLockResponse` :

```json
{ "gameId": "uuid", "holderStaffAccountId": "uuid ou null", "holderDisplayName": "string ou null", "claimedAt": "... ou null" }
```

Erreurs : `409 GAME_CONTROL_LOCKED` (prise refusée : un autre intervenant le détient déjà),
`409 GAME_CONTROL_NOT_HELD_BY_YOU` (relâchement refusé si vous n'êtes pas le détenteur) — **sauf
pour un `ADMIN`, qui peut toujours relâcher un verrou bloqué**, y compris détenu par quelqu'un
d'autre (utile si un intervenant a perdu sa connexion sans relâcher proprement).

> Ce verrou est un garde-fou UX (éviter deux intervenants qui se marchent dessus), pas une
> autorisation : les routes de pilotage elles-mêmes (parties, questions, morceaux…) ne vérifient
> pas la détention du verrou côté backend. C'est au frontend de consulter `GET /control-lock`
> avant d'afficher les actions de pilotage.

---

## 25. Projection

### `GET /api/v1/staff/events/{eventId}/projection`

**Auth : rôle `PROJECTION`, `INTERVENANT` ou `ADMIN`.** État agrégé en **lecture seule**,
pensé pour être sondé en boucle par l'écran de projection (pas de websocket). Aucune mutation
n'est possible via ce endpoint ou ce rôle.

**Réponse `200`** — `ProjectionResponse` :

```json
{
  "eventId": "uuid",
  "lobby": { "...": "LobbyResponse ou null" },
  "activeGame": { "...": "GameResponse ou null" },
  "activeTrack": { "...": "TrackStateResponse ou null, uniquement en blind test" },
  "anonymizedAnswers": [ "VotingOptionResponse[], uniquement en phase VOTE" ],
  "finalists": [ "FinalistResponse[], uniquement en phase JURY" ],
  "podium": [ "PodiumEntryResponse[]" ]
}
```

Chaque champ qui ne s'applique pas à ce qui se passe actuellement est simplement `null`/vide —
**jamais une erreur**. Le frontend doit donc toujours vérifier la présence de chaque champ avant
de rendre la section correspondante de l'écran.

---

## 26. Admin — Journal d'audit

### `GET /api/v1/admin/events/{eventId}/audit-log`

**Auth : rôle `ADMIN`.** Historique des actions administratives sensibles pour cet événement,
les plus récentes d'abord. Actuellement journalisées : suppression d'un participant, création/
modification du motif d'une exclusion `HARD` (jamais une `PREFERENCE`), régénération d'un lot
d'invitations.

**Réponse `200`** — `AuditLogEntryResponse[]` :

```json
[{
  "id": "uuid",
  "staffAccountId": "uuid",
  "staffDisplayName": "Administrateur",
  "action": "PARTICIPANT_DELETED | HARD_EXCLUSION_CREATED | HARD_EXCLUSION_REASON_UPDATED | INVITATION_BATCH_REGENERATED",
  "eventId": "uuid",
  "entityId": "uuid ou null",
  "details": "string ou null",
  "createdAt": "..."
}]
```

> `staffDisplayName` est un **instantané** du nom au moment de l'action (pas une jointure live) :
> il reste correct même si le compte staff est ensuite renommé ou supprimé. De même,
> `entityId` (participant, exclusion…) n'est **volontairement pas une clé étrangère** — l'entrée
> d'audit doit survivre à la suppression de ce qu'elle décrit.

---

## Matrice des rôles

| Route (préfixe) | Rôle requis |
|---|---|
| `GET /api/v1/events/{slug}/public` | Public |
| `GET/POST /api/v1/invitations/**` (jeton ou code de secours) | Public |
| `POST /api/v1/auth/staff/login` | Public |
| `GET /api/v1/session/me` | Toute session valide |
| `POST /api/v1/session/logout` | Aucune (idempotent) |
| `/api/v1/lobby/**` | `PARTICIPANT` |
| `/api/v1/team/me` | `PARTICIPANT` |
| `GET /api/v1/games/current` | `PARTICIPANT` |
| `/api/v1/quiz/**` | `PARTICIPANT` |
| `/api/v1/vote/**` | `PARTICIPANT` |
| `/api/v1/who-said-it/questions/**` (proposition) | `PARTICIPANT` |
| `/api/v1/staff/events/{eventId}/lobby/**` | `INTERVENANT` ou `ADMIN` |
| `/api/v1/staff/events/{eventId}/matchmaking/**` | `INTERVENANT` ou `ADMIN` |
| `/api/v1/staff/games/**`, `/api/v1/staff/questions/**` (pilotage/modération) | `INTERVENANT` ou `ADMIN` |
| `/api/v1/staff/**/tracks/**` (blind test) | `INTERVENANT` ou `ADMIN` (GET aussi `PROJECTION`) |
| `/api/v1/staff/games/{gameId}/control-lock/**` | `INTERVENANT` ou `ADMIN` |
| `/api/v1/staff/events/{eventId}/who-said-it/**` (modération/sélection) | `INTERVENANT` ou `ADMIN` |
| `/api/v1/staff/questions/{questionId}/finalists` | `JURY`, `INTERVENANT` ou `ADMIN` |
| `/api/v1/staff/questions/{questionId}/jury-decision` (GET) | `JURY`, `INTERVENANT` ou `ADMIN` |
| `/api/v1/staff/questions/{questionId}/jury-decision/**` (mutations) | `JURY` ou `ADMIN` |
| `/api/v1/staff/events/{eventId}/scores`, `POST /scores` | `INTERVENANT` ou `ADMIN` |
| `GET /api/v1/staff/events/{eventId}/podium` | `INTERVENANT`, `JURY`, `PROJECTION` ou `ADMIN` |
| `/api/v1/staff/events/{eventId}/projection` | `PROJECTION`, `INTERVENANT` ou `ADMIN` |
| `/api/v1/admin/**` (participants, invitations, exclusions, staff, personnages, événement, salon en lecture, journal d'audit) | `ADMIN` |

Toute route non listée dans la table CORS/permit-all du backend nécessite une session valide ;
un appel sans cookie valide renvoie `401`, un appel avec un rôle insuffisant renvoie `403`
(`ACCESS_DENIED`).

## Exemple d'appel (fetch)

```ts
const API_BASE = import.meta.env.VITE_API_BASE_URL; // ex: http://localhost:8080

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });
  if (!res.ok) {
    const error = await res.json().catch(() => null);
    throw new ApiError(error?.code ?? 'UNKNOWN', error?.message ?? res.statusText, res.status);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

// Confirmation d'invitation puis lecture de la session
await apiFetch(`/api/v1/invitations/${token}/confirm`, { method: 'POST' });
const me = await apiFetch<SessionMeResponse>('/api/v1/session/me');
```

## À noter pour le frontend

- Toutes les dates sont des `Instant` ISO-8601 UTC (ex. `2026-08-18T10:00:00Z`), sauf
  `eventDate` (section 11) qui est une simple `LocalDate` (`2026-12-05`).
- Tous les identifiants sont des UUID v4 en `string`.
- Aucun endpoint de jeu ne pousse d'événement en temps réel (pas de websocket/SSE) : tout ce qui
  doit rester à jour en direct (chrono de blind test, réponse d'équipe en cours de saisie, écran
  de projection, salon d'attente) est pensé pour être **sondé en boucle (polling)** par le
  frontend à intervalle raisonnable.
- `Swagger UI` (exploration interactive du contrat, toujours à jour car généré depuis le code) est
  disponible en local avec `SWAGGER_ENABLED=true` : `http://localhost:8080/swagger-ui.html` et le
  JSON brut sur `http://localhost:8080/v3/api-docs` — désactivé par défaut en production. En cas
  de doute sur un détail non couvert ici, c'est la source de vérité la plus à jour.
- Le rendu visuel du QR code (encodage de `invitationUrl` en image) est entièrement à la charge
  du frontend ; le backend ne fournit que l'URL/le jeton opaque.
- Le jeu "Lui ou Elle" a été renommé **"Who Said It"** côté backend (routes, enum `GameType`,
  codes d'erreur) : si une intégration frontend existante référence encore
  `/api/v1/lui-ou-elle/**` ou la valeur d'enum `LUI_OU_ELLE`, elle doit être mise à jour vers les
  routes de la [section 22](#22-who-said-it).
