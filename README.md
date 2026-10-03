# Cookbook

A REST API for a family cookbook: recipes, categories and ingredients, stored in PostgreSQL.

Built with Spring Boot 4.1, Java 21, Spring Data JPA and Flyway.

## Prerequisites

- Java 21
- Maven
- PostgreSQL running on `localhost:5432`

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

## API

| Method | Path                    | Description                 |
|--------|-------------------------|-----------------------------|
| GET    | `/api/recipes`          | List all recipes            |
| GET    | `/api/recipes/{id}`     | Get a recipe (404 if none)  |
| POST   | `/api/recipes`          | Create a recipe             |
| DELETE | `/api/recipes/{id}`     | Delete a recipe             |
| GET    | `/api/categories`       | List all categories         |
| GET    | `/api/categories/{id}`  | Get a category (404 if none)|
| POST   | `/api/categories`       | Create a category           |
| DELETE | `/api/categories/{id}`  | Delete a category           |
| GET    | `/api/ingredients`      | List all ingredients        |
| GET    | `/api/ingredients/{id}` | Get an ingredient (404 if none)|
| POST   | `/api/ingredients`      | Create an ingredient        |
| DELETE | `/api/ingredients/{id}` | Delete an ingredient        |

Create requests take a JSON body with a name:

```sh
curl -X POST http://localhost:8080/api/recipes \
  -H 'Content-Type: application/json' \
  -d '{"name": "Lasagna"}'
```

A blank name returns `400`. A name that already exists (ignoring case) returns `409`.

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
└── ApiExceptionHandler.java   Maps database constraint violations to 409
```
