# Cookbook

A family cookbook: recipes, categories and ingredients, stored in PostgreSQL.

- **Backend:** REST API built with Spring Boot 4.1, Java 21, Spring Data JPA and Flyway.
- **Frontend:** React app in `frontend/`, built with Vite (plain JavaScript), TanStack Query and
  React Router.

## Prerequisites

- Java 21
- Maven
- PostgreSQL running on `localhost:5432`
- Node.js 20 or newer, for the frontend (`brew install node`)

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

   On startup, Flyway creates the tables and the app listens on `http://localhost:8080`.

## Frontend

The frontend runs on its own dev server and forwards `/api` requests to the backend on port 8080,
so start the backend first. Then, in a second terminal:

```sh
cd frontend
npm install     # first time only
npm run dev     # http://localhost:5173
```

Pages:

- `/` lists recipes, with a search box and category filter. Both are kept in the URL
  (`/?search=pie&categoryId=...`), so a filtered list can be bookmarked or shared.
- `/recipes/{id}` shows a recipe with its times, categories, ingredients and numbered steps.
  Each line of a recipe's instructions is shown as one step. Edit and Delete buttons are at the top;
  Delete asks for confirmation first.
- `/recipes/new` and `/recipes/{id}/edit` are the recipe form:
  - Categories are toggled on and off, and a new one can be added right in the form.
  - Ingredient names are typed with suggestions from existing ingredients. Names that don't exist
    yet are created when the recipe is saved. Lines can be reordered, and amount and unit are optional.
  - The form checks the same rules as the backend before sending, and shows any errors the backend
    returns next to the field they belong to.

Other commands, all run in `frontend/`:

| Command              | Does                                      |
|----------------------|-------------------------------------------|
| `npm test`           | Runs the tests once (Vitest, no backend needed) |
| `npm run test:watch` | Reruns the tests on every change          |
| `npm run lint`       | Lints with oxlint                         |
| `npm run build`      | Builds the production bundle into `frontend/dist` |

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
        "imageUrl": null,
        "categoryIds": ["<category id>"],
        "ingredients": [
          {"ingredientId": "<flour id>", "amount": 250, "unit": "g"},
          {"ingredientId": "<salt id>"}
        ]
      }'
```

Ingredient lines keep the order they're sent in. `amount` and `unit` are optional, and the same
ingredient can appear on more than one line.

### Categories and ingredients

| Method | Path                    | Description                     |
|--------|-------------------------|---------------------------------|
| GET    | `/api/categories`       | List all categories             |
| GET    | `/api/categories/{id}`  | Get a category (404 if none)    |
| POST   | `/api/categories`       | Create a category               |
| DELETE | `/api/categories/{id}`  | Delete a category and remove it from its recipes |
| GET    | `/api/ingredients`      | List all ingredients            |
| GET    | `/api/ingredients/{id}` | Get an ingredient (404 if none) |
| POST   | `/api/ingredients`      | Create an ingredient            |
| DELETE | `/api/ingredients/{id}` | Delete an ingredient (409 while a recipe uses it) |

Create requests take a JSON body with a name, e.g. `{"name": "Dessert"}`.

### Errors

Errors are returned as [problem details](https://www.rfc-editor.org/rfc/rfc9457) with the reason in
`detail`:

- `400` for validation failures, with an `errors` map from field to message:
  `{"detail": "Validation failed", "errors": {"name": "must not be blank", "ingredients[0].ingredientId": "must not be null"}}`
- `400` for category or ingredient ids that don't exist
- `409` when a name already exists (ignoring case), or when deleting an ingredient that's still in use

## Database schema

The schema is managed by Flyway migrations in `src/main/resources/db/migration`. Hibernate runs
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
└── ApiExceptionHandler.java   Renders errors as problem details

frontend/src/
├── api/           fetch wrapper (client.js) and TanStack Query hooks (queries.js)
├── components/    Layout, recipe card, recipe form and its parts, shared bits
├── pages/         One component per route, with its tests next to it
├── utils/         Formatting, and recipe form logic (validation, request building)
└── test/          Test setup and helpers (renderApp, mockApi)
```
