# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). The project has no version numbers yet,
so entries are grouped by date.

## Known limitations

- Leaving the recipe form doesn't warn about unsaved changes.
- If saving a recipe fails, ingredients it created on the way stay in the database. They're reused
  on the next save, so nothing breaks.
- Error messages already on screen stay in the old language after switching; they're shown in the
  new one the next time they appear.
- Error details sent by the server for unexpected failures (5xx) are in English.
- The German and Hungarian texts haven't been reviewed by a native speaker yet. They live in
  `frontend/src/i18n/messages/`.
- Machine translations need a person's check: DeepL sometimes picks the wrong cooking meaning
  (e.g. "kemény hab", whipped cream, became "Eischnee", beaten egg whites).
- Category names, ingredient names, recipe texts and units are translated; free text inside
  ingredient units that isn't in the unit rules (e.g. "marék") stays as written.
- Translating a recipe automatically again sends the whole recipe to DeepL, even if only one line
  changed. Viewing translations never calls DeepL, and names that are already translated aren't
  sent again.
- A replaced or removed photo stays in the images folder; uploads are never deleted
  automatically.

## 2026-10-04

### Added

**Photos**

- **Photo upload** in the recipe form, with a preview and buttons to change or remove the photo; on
  phones it also offers the camera. Large photos are shrunk to at most 1600 px in the browser
  before uploading (a 10 MB phone photo becomes about 300 KB). Linking to a photo online still
  works.
- `POST /api/images` stores an uploaded photo (JPEG, PNG, WebP or GIF, checked by content, up to
  15 MB) under a generated name in the images folder and answers with its `/images/...` URL.

**Translations**

- **Recipe translations** between English, German and Hungarian, in any direction:
  - Recipes, ingredients and categories record the language they're written in (existing ones are
    Hungarian); the recipe form has a "Recipe language" field.
  - Visitors see the version in the site language where a translation exists, otherwise the
    original with a note saying which language it's in, and can switch between the two.
  - "Translate automatically" drafts a translation with DeepL, including missing ingredient and
    category names. The key (`DEEPL_API_KEY`) stays on the server; without one, only translating
    by hand is offered.
  - A translation page shows the original next to editable fields; saving marks it as reviewed.
    Translations are marked outdated when the original is edited afterwards.
  - Ingredient units follow fixed rules in other languages (ek → EL/tbsp, 25 dkg → 250 g).
  - Ingredient and category names can be translated and checked on the Categories & ingredients
    page, and are matched in any language in the recipe form ("Mehl" finds "liszt").
  - Searching also matches translated recipe names. Recipe cards show a language tag (e.g. "HU")
    when there's no translation into the site language.
- `V4__add_languages_and_translations.sql` adds the `language` columns and the
  `recipe_translation`, `recipe_translation_group`, `ingredient_translation` and
  `category_translation` tables.

**Recipes**

- **Notes** on recipes: tips, variations and footnotes in their own field, shown under the steps as
  plain paragraphs instead of being numbered as steps.
- **Ingredient groups:** each ingredient line can belong to a group such as "A tésztához", shown as
  a subheading in the list. In the form, "+ Add group heading" inserts a heading row; lines belong
  to the heading above them and a blank heading ends a group.
- `V3__add_recipe_notes_and_ingredient_groups.sql` adds `recipe.notes` and
  `recipe_ingredient.group_name`. The API has `notes` on recipes and `group` on ingredient lines.

**Look and feel**

- **New look:** a bundled serif for headings (Fraunces, works offline and covers ő/ű), a logo and
  pill navigation in a header that stays at the top, magazine-style recipe cards (four per row on
  wide screens), and a recipe page with the photo beside the title and times as pills.
- Category filter on the recipe list as chips with recipe counts, instead of a dropdown. Unused
  categories are left out.
- On the recipe page, ingredients can be ticked off while cooking, the ingredient list stays in
  view while scrolling the steps on wide screens, and notes appear as a tip box.
- A print layout for recipes, without the header and buttons.

### Changed

- API responses follow `Accept-Language`: texts come in that language where a translation exists.
  Recipe responses add `language`, `originalLanguage`, `translationStatus` and (on the detail)
  `translationOutdated`; category and ingredient list entries add `originalName`,
  `originalLanguage` and `translations`.
- Recipe, category and ingredient lists are sorted by the name shown in the requested language.

- App pages (`/` and page URLs such as `/recipes/{id}`) and photos under `/images/...` are sent with
  `Cache-Control: no-cache`, so browsers check with the server before reusing them.
- A photo that fails to load (missing file, dead link) shows the letter tile instead of a
  broken-image icon.
- The unit field in the recipe form is wider, so units like "púpozott kanál" fit, and group
  heading rows are highlighted.

### Fixed

- Photos could stay broken after the server was fixed: browsers had cached the app page that an
  older backend returned for `/images/...` and kept showing it as a broken image.

### Upgrade notes

- The V4 migration runs when the new backend starts and marks all existing recipes, ingredients
  and categories as Hungarian. A backend from before it keeps working against the migrated
  database, but shows no translations, so restart every running backend.
- For automatic translation, set `DEEPL_API_KEY` for the backend (a free DeepL API key works).
- The V3 migration runs when the new backend starts. Stop every backend still running older code
  against the same database: it doesn't show notes, and saving a recipe through it drops the
  recipe's ingredient groups.
- If photos still look broken in a browser that used the older version, clear its cache once
  (Chrome: open DevTools, right-click the reload button, "Empty Cache and Hard Reload").

## 2026-10-03

### Added

**API and database**

- **Recipe details, categories and ingredients:** a recipe can be created and updated with all of
  its fields (description, servings, prep and cook time, instructions, image URL), a list of
  category ids, and an ordered list of ingredient lines. Each line has an optional amount and unit,
  and the same ingredient can appear on more than one line.
- `PUT /api/recipes/{id}` replaces a recipe, including its categories and ingredients. Returns 404
  if the recipe doesn't exist and 409 if the new name belongs to another recipe.
- `GET /api/recipes` accepts `?search=` (name contains, any case) and `?categoryId=`, and returns
  results sorted by name.
- Category and ingredient endpoints, the same for both under `/api/categories` and
  `/api/ingredients`: list, get by id, create, rename (`PUT /{id}`) and delete. Lists are sorted by
  name and include `recipeCount`, the number of recipes using each one. Renaming returns 404 if
  missing and 409 if another one already has the name.
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
  at once, or when deleting an ingredient that a recipe still uses ("Ingredient is used by 2
  recipes").
- Validation messages follow the request's `Accept-Language` (English, German, Hungarian).

**Website** (React app in `frontend/`, plain JavaScript, built with Vite, TanStack Query and
React Router)

- Recipe list (`/`) with cards showing description, times, servings and categories. A search box
  (applied after a short pause in typing) and a category filter are kept in the URL, so a filtered
  list can be bookmarked.
- Recipe detail (`/recipes/{id}`) with total time, category links that filter the list, ingredient
  lines and numbered instruction steps (one per line of the instructions).
- Recipe form for creating (`/recipes/new`) and editing (`/recipes/{id}/edit`), linked from a
  "New recipe" button on the list and an "Edit" button on the detail page:
  - Categories as toggle chips, with a box to add a new category without leaving the form.
  - Ingredient lines with suggestions from existing ingredients; new names are created on save.
    Lines can be reordered or removed, and amount and unit are optional (`1,5` is read as 1.5).
  - Checks the backend's rules before sending, shows server errors on the matching field or
    ingredient row, and moves focus to the first problem.
- Delete button on the recipe detail page, with an in-page confirmation.
- **Categories & ingredients page** (`/manage`, linked in the header): add, filter, rename and
  delete both, with how many recipes use each. Deleting a category says how many recipes it will
  be removed from; an ingredient in use can't be deleted, and the page explains why.
- **English, German and Hungarian:** a language menu in the header translates all of the site's
  own text, including plurals, times (`1 Std. 15 Min.`, `1 óra 15 perc`) and decimal commas
  (`1,5 EL`). The first visit follows the browser's language; the choice is remembered.
- Loading, empty, error and not-found states, a layout that works on phones, and light and dark
  themes that follow the system setting.

**Build and running**

- **One app on one port:** the Maven build installs its own Node (24 LTS, into `frontend/node`),
  builds the website into the jar and runs the website's tests. `mvn spring-boot:run` and
  `java -jar` serve the website and the API together on port 8080.
- `-DskipFrontend` skips installing, building and testing the website for backend-only work;
  `-DskipTests` skips both test suites.
- `FrontendController` answers page URLs such as `/recipes/{id}/edit` with the app's `index.html`,
  so links and page reloads work. API paths, `/assets/**` and files are left alone.
- Recipe photos can be served by the app itself: files in `~/cookbook-images` (or
  `COOKBOOK_IMAGES_DIR`) are available at `/images/...`, also through the Vite dev server.
- For website work, `npm run dev` in `frontend/` starts Vite's dev server with live reload on port
  5173. It forwards `/api` to the backend on port 8080, so no CORS setup is needed.

**Tests and docs**

- Backend: Mockito tests for every service and `@WebMvcTest` tests for every controller, including
  which URLs `FrontendController` forwards. They need no database; only `CookbookApplicationTests`
  does.
- Website: Vitest and Testing Library tests for the pages, the recipe form, the categories and
  ingredients page, the language switcher, the API client and the formatting helpers, plus a check
  that every language has every text. They need no backend.
- `README.md` with setup, run modes, API reference and schema notes, and this changelog.

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
- `CreateCategoryRequest` and `CreateIngredientRequest` are now `CategoryRequest` and
  `IngredientRequest`, used for both create and rename. Names are limited to 255 characters.
- `mvn spring-boot:run` and `mvn package` now also build the website. The first run downloads Node,
  and every run takes a few seconds longer unless `-DskipFrontend` is set.

### Fixed

- The database password is read from the `POSTGRESQL_PW` environment variable.
- Entities store the name passed to their constructor and have the no-argument constructor JPA
  needs.
- Getting a missing category or recipe by id returns 404 instead of `200` with an empty body.
- Creating a category, ingredient or recipe with an existing name returns 409.
- Removed duplicate category methods from `RecipeService`.
- Removed the redundant `flyway-core` dependency (`flyway-database-postgresql` already brings it in).
- Website error messages when the server can't be reached: "Can't reach the server" instead of the
  browser's "Failed to fetch", and "The backend isn't responding" when the Vite dev server can't
  reach Spring Boot. Network errors are now retried like server errors.
- The category picker no longer says "No categories yet" next to a load error.

### Upgrade notes

- If the database was created by the old `ddl-auto: update` setup, Flyway refuses to start because
  the schema isn't empty. With no data worth keeping, drop the old tables once:
  `DROP TABLE IF EXISTS recipe, category, ingredient, flyway_schema_history CASCADE;`
- Set `POSTGRESQL_PW` in your shell (e.g. `~/.zshrc`) and in the IntelliJ run configuration.
- Recipe photos live in `~/cookbook-images` (or `COOKBOOK_IMAGES_DIR`), outside git and the jar.
  Back that folder up together with the database.
- Running from IntelliJ skips the Maven plugins, so the website is only served if it was built with
  Maven since the last clean (`mvn generate-resources` is enough). The API works either way.
