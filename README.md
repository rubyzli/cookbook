# Cookbook

A family cookbook: recipes, categories and ingredients, stored in PostgreSQL.

- **Backend:** REST API built with Spring Boot 4.1, Java 21, Spring Data JPA and Flyway.
- **Frontend:** React app in `frontend/`, built with Vite (plain JavaScript), TanStack Query and
  React Router.

## Prerequisites

- Java 21
- Maven
- PostgreSQL running on `localhost:5432`
- Node.js 20 or newer, only for working on the frontend with its dev server (`brew install node`).
  The Maven build downloads its own copy of Node, so building and running the app doesn't need it.

## Setup

1. Create the database and user:

   ```sql
   CREATE USER cookbook WITH PASSWORD 'your-password';
   CREATE DATABASE cookbook OWNER cookbook;
   ```

2. Set the database password as an environment variable:

   ```sh
   export POSTGRESQL_PW='your-password'
   ```

   Add this to `~/.zshrc` to keep it across terminals. When running from IntelliJ, set it under
   Run → Edit Configurations → Environment variables instead, since apps launched from the Dock
   don't read `~/.zshrc`.

3. Run the app:

   ```sh
   mvn spring-boot:run
   ```

   Open `http://localhost:8080`. The build first installs Node into `frontend/node` (the first run
   downloads it) and builds the frontend, so the website and the API are both served on port 8080.
   On startup, Flyway creates the tables.

   Running from IntelliJ skips the Maven plugins, so the website is only there if the frontend was
   built at least once with Maven (`mvn generate-resources`) since the last clean. The API works
   either way.

## Recipe photos

Photos are uploaded in the recipe form ("Upload photo"; on a phone this also offers the camera).
The browser shrinks large photos to at most 1600 px before uploading, and the server accepts JPEG,
PNG, WebP and GIF up to 15 MB, checked by the file's content. Uploads are stored in the database
(the `image` table) and served at `/api/images/{id}`, so they survive redeploys and are part of
every database backup. Linking to a photo online (`https://...`) still works too.

Photos used to be files in `~/cookbook-images`, served at `/images/...`. To move them into a
database, run once against it:

```sh
scripts/images-to-sql.sh ~/cookbook-images | psql "<connection string>"
```

It's safe to run again; it only touches recipes that still point at `/images/...`.

## Translating recipes

Every recipe, ingredient and category is stored in the language it was written in (English, German
or Hungarian), and can have a translation into each of the others. Visitors see the version in the
site language when one exists, and otherwise the original, marked with its language.

- **Automatically:** with a [DeepL API](https://www.deepl.com/pro-api) key, saving a recipe
  machine-translates it into the other languages in the background, including its ingredient and
  category names that have none yet. A language is redone after an edit only if its translation is
  a machine one; reviewed translations are never replaced, just marked outdated. If DeepL fails,
  the recipe is still saved and the failure is logged. "Translate automatically" on a recipe page
  does the same for one language on demand. The free plan allows 500,000+ characters a month; all recipes so far are about
  13,000. Set the key as `DEEPL_API_KEY` for the backend (shell, IntelliJ run configuration or
  server). It's only used by the server, never sent to the browser. Without a key, everything works
  except the automatic buttons.
- **By hand:** the translation page (`/recipes/{id}/translate/{language}`) shows the original next
  to editable fields. Saving marks the translation as reviewed; machine drafts stay marked
  "machine translation" until then.
- **Staying in sync:** if the original is edited after translating, the translation is marked as
  outdated until it's saved again.
- **Units** are converted by fixed rules, not by DeepL: ek ↔ EL ↔ tbsp, kk ↔ TL ↔ tsp, db ↔ Stk.,
  csomag ↔ Pck., and dekagrams become grams outside Hungarian (`frontend/src/utils/units.js`).
- **Ingredient and category names** can be checked and corrected on the Categories & ingredients
  page, which also has "Translate missing names automatically".

## Building a jar

```sh
mvn package
java -jar target/cookbook-0.0.1-SNAPSHOT.jar
```

`mvn package` builds and tests both the backend and the frontend, and puts the built frontend inside
the jar, so the jar plus a PostgreSQL database is everything needed to run the app. Useful flags:

| Flag             | Effect                                                              |
|------------------|---------------------------------------------------------------------|
| `-DskipTests`    | Skips the backend and frontend tests                                |
| `-DskipFrontend` | Skips installing, building and testing the frontend (backend-only work) |

## Frontend

When working on the frontend, use Vite's dev server: it reloads the page as you edit. It forwards
`/api` requests to the backend on port 8080, so start the backend first. Then, in a second terminal:

```sh
cd frontend
npm install     # first time only
npm run dev     # http://localhost:5173
```

Pages:

- `/` lists recipes, with a search box and category filter. Both are kept in the URL
  (`/?search=pie&categoryId=...`), so a filtered list can be bookmarked or shared.
- `/recipes/{id}` shows a recipe with its times, categories, ingredients and numbered steps.
  Each line of a recipe's instructions is shown as one step. Ingredients with a group are listed
  under its heading (e.g. "For the dough"), and notes appear in their own section under the steps. Edit and Delete buttons are at the top;
  Delete asks for confirmation first.
- `/manage` lists categories and ingredients side by side, with how many recipes use each. Both
  can be added, filtered and renamed. Deleting a category removes it from its recipes (the
  confirmation says how many); an ingredient can't be deleted while a recipe uses it.
- `/recipes/new` and `/recipes/{id}/edit` are the recipe form:
  - Categories are toggled on and off, and a new one can be added right in the form.
  - Group headings ("+ Add group heading") split the ingredient list; lines belong to the heading
    above them, and a blank heading ends a group. Notes have their own field under the steps.
  - Ingredient names are typed with suggestions from existing ingredients. Names that don't exist
    yet are created when the recipe is saved. Lines can be reordered, and amount and unit are optional.
  - The form checks the same rules as the backend before sending, and shows any errors the backend
    returns next to the field they belong to.

The site is available in English, German and Hungarian, picked with the menu in the header. The
first visit uses the browser's language (English if it isn't one of the three) and the choice is
remembered in the browser. Only the site's own text is translated; recipes appear as entered. The
texts live in `frontend/src/i18n/messages/`, one file per language with the same keys; a test
checks that every language has every key. API requests send the language as `Accept-Language`, so
the backend's validation messages match it.

Other commands, all run in `frontend/`:

| Command              | Does                                      |
|----------------------|-------------------------------------------|
| `npm test`           | Runs the tests once (Vitest, no backend needed) |
| `npm run test:watch` | Reruns the tests on every change          |
| `npm run lint`       | Lints with oxlint                         |
| `npm run build`      | Builds the production bundle into `frontend/dist` (Maven builds into the jar instead) |

Page URLs like `/recipes/{id}` are handled by React in the browser. When one is opened directly
from the jar, `FrontendController` answers with `index.html` so the right page still loads.

## API

### Recipes

| Method | Path                | Description                                                        |
|--------|---------------------|--------------------------------------------------------------------|
| GET    | `/api/recipes`      | List recipes as summaries, sorted by name. Optional `?search=` (name contains, any case) and `?categoryId=` |
| GET    | `/api/recipes/{id}` | Full recipe with categories and ingredient lines (404 if none)     |
| POST   | `/api/recipes`      | Create a recipe (201)                                              |
| PUT    | `/api/recipes/{id}` | Replace a recipe, including its categories and ingredients (404 if none) |
| DELETE | `/api/recipes/{id}` | Delete a recipe (204)                                              |

POST and PUT take the same body. Only `name` is required; missing lists count as empty:

```sh
curl -X POST http://localhost:8080/api/recipes \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Apple Pie",
        "description": "Grandma'"'"'s recipe",
        "servings": 8,
        "prepTimeMinutes": 30,
        "cookTimeMinutes": 45,
        "instructions": "Mix. Bake.",
        "notes": "Use tart apples.",
        "imageUrl": null,
        "categoryIds": ["<category id>"],
        "ingredients": [
          {"ingredientId": "<flour id>", "amount": 250, "unit": "g", "group": "For the dough"},
          {"ingredientId": "<salt id>"}
        ]
      }'
```

Ingredient lines keep the order they're sent in. `amount`, `unit` and `group` are optional, and the
same ingredient can appear on more than one line. `group` (up to 100 characters) is the heading a
line is listed under; consecutive lines with the same group are shown together. `notes` holds tips
and variations, shown separately from the numbered steps.

### Categories and ingredients

| Method | Path                    | Description                     |
|--------|-------------------------|---------------------------------|
| GET    | `/api/categories`       | List all categories by name, each with `recipeCount` |
| GET    | `/api/categories/{id}`  | Get a category (404 if none)    |
| POST   | `/api/categories`       | Create a category               |
| PUT    | `/api/categories/{id}`  | Rename a category (404 if none, 409 if the name is taken) |
| DELETE | `/api/categories/{id}`  | Delete a category and remove it from its recipes |
| GET    | `/api/ingredients`      | List all ingredients by name, each with `recipeCount` |
| GET    | `/api/ingredients/{id}` | Get an ingredient (404 if none) |
| POST   | `/api/ingredients`      | Create an ingredient            |
| PUT    | `/api/ingredients/{id}` | Rename an ingredient (404 if none, 409 if the name is taken) |
| DELETE | `/api/ingredients/{id}` | Delete an ingredient (409 while a recipe uses it) |

Create and rename take a JSON body with a name, and on create optionally the language it's written
in, e.g. `{"name": "Dessert", "language": "en"}` (Hungarian if left out). List entries look like
`{"id": "...", "name": "Dessert", "originalName": "Dessert", "originalLanguage": "en",
"recipeCount": 3, "translations": {"de": {"name": "Nachspeise", "status": "MACHINE"}}}`.

### Languages and translations

Reads use the `Accept-Language` header: `name`, steps and so on come in that language where a
translation exists. Responses say which language they are in (`language`), what the original is
(`originalLanguage`), and for recipes `translationStatus` (`MACHINE`, `REVIEWED` or `null`) and
`translationOutdated`. `GET /api/recipes/{id}?original=true` always returns the original, for
editing. Recipes take a `language` field (`en`, `de` or `hu`).

| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/translations/settings` | Whether machine translation is set up, and the languages |
| GET    | `/api/recipes/{id}/translations` | The original text and all translations of a recipe |
| POST   | `/api/recipes/{id}/translations/{lang}/machine` | Machine-translate a recipe (replaces an existing translation) |
| PUT    | `/api/recipes/{id}/translations/{lang}` | Save a translation, by default as reviewed |
| DELETE | `/api/recipes/{id}/translations/{lang}` | Delete a translation |
| POST   | `/api/images` | Upload a photo (multipart field `file`); answers `{"url": "/api/images/{id}"}` for a recipe's `imageUrl` |
| GET    | `/api/images/{id}` | A stored photo |
| PUT/DELETE | `/api/{ingredients,categories}/{id}/translations/{lang}` | Set or remove a name in another language |
| POST   | `/api/{ingredients,categories}/translations/{lang}/machine` | Machine-translate all names that have no translation in that language |

### Errors

Errors are returned as [problem details](https://www.rfc-editor.org/rfc/rfc9457) with the reason in
`detail`:

- `400` for validation failures, with an `errors` map from field to message:
  `{"detail": "Validation failed", "errors": {"name": "must not be blank", "ingredients[0].ingredientId": "must not be null"}}`
- `400` for category or ingredient ids that don't exist
- `409` when a name already exists (ignoring case), or when deleting an ingredient that's still in use

## Database schema

The schema is managed by Flyway migrations in `backend`. Hibernate runs
with `ddl-auto: validate`, so it checks the entities against the tables at startup but never
changes them.

To change the schema, add a new migration file (`V2__describe_change.sql`, `V3__...`) and update
the matching entity. Don't edit migrations that have already run.

## Project structure

```
src/main/java/family/cookbook/
├── category/      Category entity, repository, service, controller
├── ingredient/    Ingredient entity, repository, service, controller
├── recipe/        Recipe entity, repository, service, controller
├── image/         Photo uploads, stored in the database and served at /api/images/{id}
├── translation/   Languages, translation tables, DeepL client, translation endpoints
├── ApiExceptionHandler.java   Renders errors as problem details
└── FrontendController.java    Serves the React app for its page URLs

frontend/src/
├── api/           fetch wrapper (client.js) and TanStack Query hooks (queries.js)
├── components/    Layout, recipe card, recipe form and its parts, editable name lists
├── i18n/          Site texts (messages/en.js, de.js, hu.js) and the language switcher logic
├── pages/         One component per route, with its tests next to it
├── utils/         Formatting, unit rules for other languages, recipe form logic
└── test/          Test setup and helpers (renderApp, mockApi)
```
