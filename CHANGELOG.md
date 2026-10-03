# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). The project has no version numbers yet,
so entries are grouped by date.

## 2026-10-03

### Added

- **Frontend** in `frontend/`: a React app (plain JavaScript) built with Vite, using TanStack
  Query for API calls and React Router for pages. In development, Vite forwards `/api` to the
  backend on port 8080, so no CORS setup is needed.
  - Recipe list (`/`) with cards showing description, times, servings and categories. A search box
    (applied after a short pause in typing) and a category filter are kept in the URL.
  - Recipe detail (`/recipes/{id}`) with total time, category links that filter the list,
    ingredient lines and numbered instruction steps.
  - Loading, empty, error and not-found states, a layout that works on phones, and light and dark
    themes that follow the system setting.
  - Vitest and Testing Library tests for the pages, API client and formatting helpers.
- **Recipe form** for creating (`/recipes/new`) and editing (`/recipes/{id}/edit`) recipes, linked
  from a "New recipe" button on the list and an "Edit" button on the detail page.
  - Categories as toggle chips, with a box to add a new category without leaving the form.
  - Ingredient lines with suggestions from existing ingredients; new names are created on save.
    Lines can be reordered or removed, and amount and unit are optional (`1,5` is read as 1.5).
  - Checks the backend's rules before sending, shows server errors on the matching field or
    ingredient row, and moves focus to the first problem.
- Delete button on the recipe detail page, with an in-page confirmation.
- **Recipe details, categories and ingredients:** a recipe can now be created and updated with all
  of its fields (description, servings, prep and cook time, instructions, image URL), a list of
  category ids, and an ordered list of ingredient lines. Each line has an optional amount and unit,
  and the same ingredient can appear on more than one line.
- `PUT /api/recipes/{id}` replaces a recipe, including its categories and ingredients. Returns 404
  if the recipe doesn't exist and 409 if the new name belongs to another recipe.
- `GET /api/recipes` accepts `?search=` (name contains, any case) and `?categoryId=`, and returns
  results sorted by name.
- Ingredient endpoints: `GET`, `GET /{id}`, `POST` and `DELETE` under `/api/ingredients`.
- Flyway migrations own the database schema:
  - `V1__create_tables.sql` creates `category`, `ingredient` and `recipe`, with names unique
    regardless of case.
  - `V2__link_recipes_to_categories_and_ingredients.sql` adds `recipe_category` and
    `recipe_ingredient`.
- Request validation: names must not be blank, servings must be positive, times and amounts must
  not be negative, and length limits match the database columns.
- Error responses use the [problem details](https://www.rfc-editor.org/rfc/rfc9457) format with
  the reason in `detail`. Validation errors also include an `errors` map from field to message,
  e.g. `"ingredients[0].ingredientId": "must not be null"`.
- 400 response listing any category or ingredient ids in a recipe request that don't exist.
- 409 response when a name already exists (ignoring case), when two requests create the same name
  at once, or when deleting an ingredient that a recipe still uses.
- Unit tests: Mockito tests for every service and `@WebMvcTest` tests for every controller. They
  need no database; only `CookbookApplicationTests` does.
- `README.md` with setup, API reference and schema notes.
- This changelog.

### Changed

- `GET /api/recipes` returns summaries without instructions or ingredients, and
  `GET /api/recipes/{id}` returns the full recipe. Responses are now dedicated records
  (`RecipeSummary`, `RecipeDetail`) rather than the JPA entity.
- `POST /api/recipes` takes the new `RecipeRequest` body, which replaces `CreateRecipeRequest`.
  Only `name` is required, so existing `{"name": "..."}` requests still work.
- Hibernate runs with `ddl-auto: validate` instead of `update`, so it checks the entities against
  the tables at startup and never changes the schema.
- Deleting a category removes it from its recipes instead of failing.
- Recipe numeric fields use `Integer`, and `createdAt` is an `Instant` set automatically on insert.

### Fixed

- Frontend error messages when the server can't be reached: "Can't reach the server" instead of
  the browser's "Failed to fetch", and "The backend isn't responding" when the Vite proxy can't
  reach Spring Boot. Network errors are now retried like server errors.
- The category picker no longer says "No categories yet" next to a load error.

- The database password is read from the `POSTGRESQL_PW` environment variable.
- Entities store the name passed to their constructor and have the no-argument constructor JPA
  needs.
- Getting a missing category or recipe by id returns 404 instead of `200` with an empty body.
- Creating a category, ingredient or recipe with an existing name returns 409.
- Removed duplicate category methods from `RecipeService`.
- Removed the redundant `flyway-core` dependency (`flyway-database-postgresql` already brings it in).

### Upgrade notes

- If the database was created by the old `ddl-auto: update` setup, Flyway refuses to start because
  the schema isn't empty. With no data worth keeping, drop the old tables once:
  `DROP TABLE IF EXISTS recipe, category, ingredient, flyway_schema_history CASCADE;`
- Set `POSTGRESQL_PW` in your shell (e.g. `~/.zshrc`) and in the IntelliJ run configuration.
