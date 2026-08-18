# Guide d'intégration API — Frontend

Documentation à destination de l'équipe frontend pour consommer l'API `wedding-games-back`.
Toutes les routes sont préfixées par `/api/v1`. Le format d'échange est **JSON** partout,
sauf indication contraire.

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
- [Matrice des rôles](#matrice-des-rôles)

---

## Authentification & session

Il n'y a **pas de JWT ni de header `Authorization`**. L'authentification repose sur un
**cookie de session opaque, `HttpOnly`**, posé automatiquement par le navigateur.

- Nom du cookie : `wg_session` (configurable côté backend, mais c'est la valeur par défaut).
- Le cookie est posé par le backend via `Set-Cookie` lors de :
  - `POST /api/v1/invitations/{token}/confirm` (invité),
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
  - **`PARTICIPANT`** : un invité, authentifié uniquement via le jeton d'invitation (QR code),
    jamais par mot de passe.
  - **`STAFF`** : un membre de l'organisation (`ADMIN`, `INTERVENANT`, `JURY`, `PROJECTION`),
    authentifié par identifiant/mot de passe.

Codes HTTP transverses liés à l'auth :

| Cas | Code |
|---|---|
| Pas de cookie / cookie invalide ou expiré sur une route protégée | `401 Unauthorized` |
| Cookie valide mais rôle insuffisant pour la route | `403 Forbidden` |
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

Codes rencontrés dans l'API :

| `code` | HTTP | Où |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Corps de requête invalide (`@Valid` échoué) — voir `details[]` |
| `SAME_PARTICIPANT` | 400 | Création d'exclusion avec `participantAId == participantBId` |
| `RESOURCE_NOT_FOUND` | 404 | Ressource (événement, participant, compte, exclusion, invitation…) introuvable |
| `INVALID_INVITATION` | 404 | Jeton d'invitation inconnu ou révoqué |
| `EXCLUSION_ALREADY_EXISTS` | 409 | Exclusion déjà existante entre les deux participants |
| `USERNAME_TAKEN` | 409 | Nom d'utilisateur staff déjà pris |
| `HARD_EXCLUSION_IMMUTABLE` | 409 | Tentative de suppression d'une exclusion `HARD` (interdit, définitif) |
| `ACCESS_DENIED` | 403 | Rôle insuffisant |
| `INVALID_CREDENTIALS` | 401 | Login staff incorrect |
| `INTERNAL_ERROR` | 500 | Erreur inattendue |

## Enums

```ts
type EventStatus = /* voir WeddingEvent — champ status renvoyé tel quel */ string;

type ParticipantType = 'GUEST' | 'SPOUSE' | 'ORGANIZER';
type ParticipantStatus = 'INVITED' | 'CONFIRMED' | 'CONNECTED' | 'PAUSED' | 'ABSENT';

type InvitationStatus = 'ACTIVE' | 'REVOKED';

type LobbyStatus = 'CLOSED' | 'OPEN' | 'LOCKED';
type LobbyConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'LATE';

type ExclusionType = 'HARD' | 'PREFERENCE'; // HARD = jamais contournable, PREFERENCE = indicatif

type StaffRole = 'ADMIN' | 'INTERVENANT' | 'JURY' | 'PROJECTION';
// Role effectif de session (staff ou participant) :
type Role = 'ADMIN' | 'INTERVENANT' | 'JURY' | 'PARTICIPANT' | 'PROJECTION';
type ActorType = 'PARTICIPANT' | 'STAFF';
```

> Note : `JURY` et `PROJECTION` existent comme rôles mais aucun endpoint ne les utilise encore
> (réservés à une phase future — jeux/scores).

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

Flux de l'invité scannant son QR code — **aucune authentification préalable requise** pour ces
deux routes.

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
`actorType`). Utiliser `actorType`/`role` pour aiguiller le routing frontend (écran invité vs
back-office).

### `POST /api/v1/session/logout`

**Auth : aucune vérification de rôle** (fonctionne même avec un cookie déjà expiré/absent,
idempotent). Révoque la session côté serveur et efface le cookie.

**Réponse `204 No Content`.**

---

## 5. Salon d'attente — participant

### `POST /api/v1/lobby/heartbeat`

**Auth : rôle `PARTICIPANT`.** À appeler périodiquement (ex. toutes les 10-15s) tant que
l'invité est sur l'écran de salon d'attente, pour signaler sa présence. Aucun corps de requête ;
l'identité vient du cookie de session.

**Réponse `200`** — `LobbyParticipantResponse`

```json
{
  "participantId": "uuid",
  "displayName": "Jessika Dijoux",
  "connectionStatus": "CONNECTED",
  "arrivedAt": "2026-08-18T10:00:00Z",
  "lastActivityAt": "2026-08-18T10:05:00Z"
}
```

---

## 6. Salon d'attente — staff (intervenant/admin)

Base : `/api/v1/staff/events/{eventId}/lobby`
**Auth : rôle `INTERVENANT` ou `ADMIN`.**

| Méthode | Route | Description | Réponse |
|---|---|---|---|
| `POST` | `/open` | Ouvre le salon (statut → `OPEN`) | `LobbyResponse` |
| `POST` | `/close` | Ferme le salon (statut → `CLOSED`) | `LobbyResponse` |
| `POST` | `/lock` | Verrouille le salon (statut → `LOCKED`, plus d'admission) | `LobbyResponse` |
| `GET` | `/participants` | Liste tous les participants présents/inscrits dans le salon | `LobbyParticipantResponse[]` |
| `POST` | `/participants/{participantId}/late` | Marque un participant en retard (`connectionStatus: LATE`) | `LobbyParticipantResponse` |
| `POST` | `/participants/{participantId}/admit` | Admet/réadmet un participant (`connectionStatus: CONNECTED`) | `LobbyParticipantResponse` |

`LobbyResponse` :

```json
{ "id": "uuid", "eventId": "uuid", "status": "OPEN", "openedAt": "...", "closedAt": null }
```

Note : le salon est créé implicitement (`getOrCreate`) au premier appel sur un événement — pas
besoin d'endpoint de création dédié. Idem pour `/participants/{id}/late` et `/admit` : si le
participant n'a pas encore d'entrée dans le salon, elle est créée à la volée.

Il existe aussi une variante **lecture seule pour l'admin**, sans le pilotage
ouverture/fermeture :

- `GET /api/v1/admin/events/{eventId}/lobby` (rôle `ADMIN`) → `LobbyResponse`
- `GET /api/v1/admin/events/{eventId}/lobby/participants` (rôle `ADMIN`) → `LobbyParticipantResponse[]`

---

## 7. Admin — Participants

Base : `/api/v1/admin` — **Auth : rôle `ADMIN`.**

### `GET /events/{eventId}/participants`

Liste tous les participants d'un événement. Réponse : `ParticipantResponse[]`.

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

**Réponse `204 No Content`.**

---

## 8. Admin — Invitations

Base : `/api/v1/admin/participants/{participantId}/invitation` — **Auth : rôle `ADMIN`.**

### `POST /`

Génère une nouvelle invitation pour ce participant, **invalidant automatiquement l'ancien
jeton actif** s'il existait (une seule invitation active à la fois par participant).

**Réponse `201`** — `InvitationAdminResponse` :

```json
{
  "invitationId": "uuid",
  "participantId": "uuid",
  "rawToken": "chaîne opaque base64url",
  "invitationUrl": "http://.../invite/<rawToken>",
  "createdAt": "..."
}
```

> ⚠️ **`rawToken`/`invitationUrl` ne sont retournés qu'une seule fois, ici.** Le backend ne
> stocke que le hash SHA-256 du jeton et ne peut jamais le re-livrer. Si le frontend a besoin de
> régénérer un QR code, il faut rappeler cette route (ce qui révoquera l'ancien jeton) — pas de
> route "récupérer le jeton actuel en clair".
>
> C'est ce champ `invitationUrl` (ou `rawToken`) que le frontend encode dans le QR code affiché/
> imprimé pour l'invité. Le rendu visuel du QR code (génération de l'image) est entièrement à la
> charge du frontend.

### `GET /`

Consulte le statut de l'invitation active du participant, **sans exposer le jeton**
(à utiliser pour afficher "invitation envoyée / jeton valide" dans le back-office, sans pouvoir
reconstruire l'URL).

**Réponse `200`** — `InvitationStatusResponse` :

```json
{ "invitationId": "uuid", "status": "ACTIVE", "createdAt": "..." }
```

Erreurs : `404 RESOURCE_NOT_FOUND` si aucune invitation active n'existe pour ce participant.

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

### `GET /api/v1/admin/exclusions/{id}`

**Réponse `200`** — `PairingExclusionResponse`.

### `PATCH /api/v1/admin/exclusions/{id}`

Met à jour uniquement le motif (`reason`). **Corps** — `PairingExclusionReasonUpdateRequest` :

```json
{ "reason": "string, optionnel, max 300" }
```

**Réponse `200`** — `PairingExclusionResponse`.

### `DELETE /api/v1/admin/exclusions/{id}`

**Réponse `204 No Content`.**

Erreurs : `409 HARD_EXCLUSION_IMMUTABLE` — **une exclusion `HARD` ne peut jamais être supprimée
via l'API**, quel que soit l'appelant. Le frontend doit désactiver/masquer le bouton de
suppression pour les lignes où `exclusionType === 'HARD'` (ou `locked === true`) plutôt que de
laisser l'utilisateur cliquer pour se prendre un 409.

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

## Matrice des rôles

| Route (préfixe) | Rôle requis |
|---|---|
| `GET /api/v1/events/{slug}/public` | Public |
| `GET/POST /api/v1/invitations/{token}/...` | Public |
| `POST /api/v1/auth/staff/login` | Public |
| `GET /api/v1/session/me` | Toute session valide |
| `POST /api/v1/session/logout` | Aucune (idempotent) |
| `POST /api/v1/lobby/heartbeat` | `PARTICIPANT` |
| `/api/v1/staff/events/{eventId}/lobby/**` | `INTERVENANT` ou `ADMIN` |
| `/api/v1/admin/**` (participants, invitations, exclusions, staff, lobby en lecture) | `ADMIN` |

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

- Toutes les dates sont des `Instant` ISO-8601 UTC (ex. `2026-08-18T10:00:00Z`).
- Tous les identifiants sont des UUID v4 en `string`.
- `Swagger UI` (exploration interactive du contrat) est disponible en local avec
  `SWAGGER_ENABLED=true` : `http://localhost:8080/swagger-ui.html` et le JSON brut sur
  `http://localhost:8080/v3/api-docs` — désactivé par défaut en production.
- Le rendu visuel du QR code (encodage de `invitationUrl` en image) est entièrement à la charge
  du frontend ; le backend ne fournit que l'URL/le jeton opaque.
