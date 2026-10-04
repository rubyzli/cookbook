package family.cookbook.translation;

import family.cookbook.category.Category;
import family.cookbook.category.CategoryRepository;
import family.cookbook.ingredient.Ingredient;
import family.cookbook.ingredient.IngredientRepository;
import family.cookbook.recipe.Recipe;
import family.cookbook.recipe.RecipeIngredient;
import family.cookbook.recipe.RecipeRepository;
import family.cookbook.translation.dto.MachineFillResult;
import family.cookbook.translation.dto.RecipeTranslationRequest;
import family.cookbook.translation.dto.RecipeTranslationResponse;
import family.cookbook.translation.dto.RecipeTranslations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TranslationService {

    private final RecipeRepository recipeRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeTranslationRepository recipeTranslations;
    private final CategoryTranslationRepository categoryTranslations;
    private final IngredientTranslationRepository ingredientTranslations;
    private final MachineTranslator translator;

    public TranslationService(RecipeRepository recipeRepository,
                              CategoryRepository categoryRepository,
                              IngredientRepository ingredientRepository,
                              RecipeTranslationRepository recipeTranslations,
                              CategoryTranslationRepository categoryTranslations,
                              IngredientTranslationRepository ingredientTranslations,
                              MachineTranslator translator) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeTranslations = recipeTranslations;
        this.categoryTranslations = categoryTranslations;
        this.ingredientTranslations = ingredientTranslations;
        this.translator = translator;
    }

    public boolean machineTranslationEnabled() {
        return translator.isEnabled();
    }

    @Transactional(readOnly = true)
    public Optional<RecipeTranslations> getTranslations(UUID recipeId) {
        return recipeRepository.findWithDetailsById(recipeId).map(recipe -> {
            String hash = SourceText.hash(recipe);
            return new RecipeTranslations(
                    recipe.getLanguage(),
                    new RecipeTranslations.Original(recipe.getName(), recipe.getDescription(),
                            recipe.getInstructions(), recipe.getNotes(), SourceText.groups(recipe)),
                    recipeTranslations.findByIdOwnerIdOrderByIdLanguage(recipeId).stream()
                            .map(t -> RecipeTranslationResponse.from(t, hash))
                            .toList());
        });
    }

    // Machine-translates the recipe, replacing any earlier translation into that language, and also
    // translates its ingredient and category names that have no translation in that language yet
    @Transactional
    public Optional<RecipeTranslationResponse> machineTranslate(UUID recipeId, String language) {
        Optional<Recipe> found = recipeRepository.findWithDetailsById(recipeId);
        if (found.isEmpty()) return Optional.empty();
        Recipe recipe = found.get();
        requireOtherLanguage(recipe, language);

        List<String> instructionLines = lines(recipe.getInstructions());
        List<String> noteLines = lines(recipe.getNotes());
        List<String> groups = SourceText.groups(recipe);
        List<String> texts = new ArrayList<>();
        texts.add(recipe.getName());
        texts.add(recipe.getDescription());
        texts.addAll(instructionLines);
        texts.addAll(noteLines);
        texts.addAll(groups);

        List<String> translated = translator.translate(texts, recipe.getLanguage(), language);
        int at = 0;
        String name = translated.get(at++);
        String description = translated.get(at++);
        String instructions = joinLines(recipe.getInstructions(), translated.subList(at, at += instructionLines.size()));
        String notes = joinLines(recipe.getNotes(), translated.subList(at, at += noteLines.size()));
        Map<String, String> groupTranslations = new LinkedHashMap<>();
        for (String group : groups) {
            groupTranslations.put(group, translated.get(at++));
        }

        RecipeTranslation translation = recipeTranslations.findById(new TranslationKey(recipeId, language))
                .orElseGet(() -> new RecipeTranslation(recipeId, language));
        translation.update(name, description, instructions, notes, groupTranslations,
                TranslationStatus.MACHINE, SourceText.hash(recipe));
        recipeTranslations.save(translation);

        translateMissingIngredientNames(recipe.getIngredients().stream().map(RecipeIngredient::getIngredient).toList(),
                language);
        translateMissingCategoryNames(List.copyOf(recipe.getCategories()), language);
        return Optional.of(RecipeTranslationResponse.from(translation, SourceText.hash(recipe)));
    }

    // The languages to machine-translate a recipe into after it was saved: those with no translation
    // yet, or a machine translation of text that has changed since. Reviewed translations are left
    // alone even when outdated, so nobody's corrections are overwritten.
    @Transactional(readOnly = true)
    public List<String> languagesToTranslateAutomatically(UUID recipeId) {
        if (!translator.isEnabled()) return List.of();
        return recipeRepository.findWithDetailsById(recipeId).map(recipe -> {
            String hash = SourceText.hash(recipe);
            Map<String, RecipeTranslation> existing = recipeTranslations.findByIdOwnerIdOrderByIdLanguage(recipeId)
                    .stream().collect(Collectors.toMap(RecipeTranslation::getLanguage, Function.identity()));
            return Languages.SUPPORTED.stream()
                    .filter(language -> !language.equals(recipe.getLanguage()))
                    .filter(language -> {
                        RecipeTranslation translation = existing.get(language);
                        return translation == null || (translation.getStatus() == TranslationStatus.MACHINE
                                && !hash.equals(translation.getSourceHash()));
                    })
                    .sorted()
                    .toList();
        }).orElse(List.of());
    }

    @Transactional
    public Optional<RecipeTranslationResponse> saveTranslation(UUID recipeId, String language,
                                                              RecipeTranslationRequest request) {
        Optional<Recipe> found = recipeRepository.findWithDetailsById(recipeId);
        if (found.isEmpty()) return Optional.empty();
        Recipe recipe = found.get();
        requireOtherLanguage(recipe, language);
        // Only headings the recipe still has; blank ones mean "not translated"
        Map<String, String> groups = new LinkedHashMap<>();
        for (String group : SourceText.groups(recipe)) {
            String value = request.groups().get(group);
            if (value != null && !value.isBlank()) groups.put(group, value.strip());
        }
        RecipeTranslation translation = recipeTranslations.findById(new TranslationKey(recipeId, language))
                .orElseGet(() -> new RecipeTranslation(recipeId, language));
        translation.update(request.name().strip(), blankToNull(request.description()), blankToNull(request.instructions()),
                blankToNull(request.notes()), groups, request.status(), SourceText.hash(recipe));
        recipeTranslations.save(translation);
        return Optional.of(RecipeTranslationResponse.from(translation, SourceText.hash(recipe)));
    }

    @Transactional
    public void deleteTranslation(UUID recipeId, String language) {
        recipeTranslations.deleteById(new TranslationKey(recipeId, language));
    }

    @Transactional
    public boolean setIngredientName(UUID ingredientId, String language, String name) {
        Optional<Ingredient> ingredient = ingredientRepository.findById(ingredientId);
        if (ingredient.isEmpty()) return false;
        requireOtherLanguage(ingredient.get().getLanguage(), language);
        IngredientTranslation translation = ingredientTranslations.findById(new TranslationKey(ingredientId, language))
                .orElseGet(() -> new IngredientTranslation(ingredientId, language, name, TranslationStatus.REVIEWED));
        translation.update(name.strip(), TranslationStatus.REVIEWED);
        ingredientTranslations.save(translation);
        return true;
    }

    @Transactional
    public boolean setCategoryName(UUID categoryId, String language, String name) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (category.isEmpty()) return false;
        requireOtherLanguage(category.get().getLanguage(), language);
        CategoryTranslation translation = categoryTranslations.findById(new TranslationKey(categoryId, language))
                .orElseGet(() -> new CategoryTranslation(categoryId, language, name, TranslationStatus.REVIEWED));
        translation.update(name.strip(), TranslationStatus.REVIEWED);
        categoryTranslations.save(translation);
        return true;
    }

    @Transactional
    public void deleteIngredientName(UUID ingredientId, String language) {
        ingredientTranslations.deleteById(new TranslationKey(ingredientId, language));
    }

    @Transactional
    public void deleteCategoryName(UUID categoryId, String language) {
        categoryTranslations.deleteById(new TranslationKey(categoryId, language));
    }

    @Transactional
    public MachineFillResult translateMissingIngredientNames(String language) {
        return new MachineFillResult(translateMissingIngredientNames(ingredientRepository.findAll(), language));
    }

    @Transactional
    public MachineFillResult translateMissingCategoryNames(String language) {
        return new MachineFillResult(translateMissingCategoryNames(categoryRepository.findAll(), language));
    }

    private int translateMissingIngredientNames(List<Ingredient> ingredients, String language) {
        var existing = ingredientTranslations.findByIdLanguage(language).stream()
                .map(IngredientTranslation::getIngredientId).collect(Collectors.toSet());
        List<Ingredient> missing = ingredients.stream()
                .filter(i -> !i.getLanguage().equals(language) && !existing.contains(i.getId()))
                .distinct()
                .toList();
        Map<UUID, String> names = translateNames(missing, Ingredient::getId, Ingredient::getName, Ingredient::getLanguage,
                language);
        names.forEach((id, name) ->
                ingredientTranslations.save(new IngredientTranslation(id, language, name, TranslationStatus.MACHINE)));
        return names.size();
    }

    private int translateMissingCategoryNames(List<Category> categories, String language) {
        var existing = categoryTranslations.findByIdLanguage(language).stream()
                .map(CategoryTranslation::getCategoryId).collect(Collectors.toSet());
        List<Category> missing = categories.stream()
                .filter(c -> !c.getLanguage().equals(language) && !existing.contains(c.getId()))
                .distinct()
                .toList();
        Map<UUID, String> names = translateNames(missing, Category::getId, Category::getName, Category::getLanguage,
                language);
        names.forEach((id, name) ->
                categoryTranslations.save(new CategoryTranslation(id, language, name, TranslationStatus.MACHINE)));
        return names.size();
    }

    // One request per source language, since names can have been entered in different languages
    private <T> Map<UUID, String> translateNames(List<T> items, Function<T, UUID> id, Function<T, String> name,
                                                 Function<T, String> sourceLanguage, String language) {
        Map<UUID, String> result = new LinkedHashMap<>();
        items.stream().collect(Collectors.groupingBy(sourceLanguage)).forEach((source, group) -> {
            List<String> translated = translator.translate(group.stream().map(name).toList(), source, language);
            for (int i = 0; i < group.size(); i++) {
                result.put(id.apply(group.get(i)), translated.get(i).strip());
            }
        });
        return result;
    }

    private static void requireOtherLanguage(Recipe recipe, String language) {
        requireOtherLanguage(recipe.getLanguage(), language);
    }

    private static void requireOtherLanguage(String original, String language) {
        if (original.equals(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This is already written in that language");
        }
    }

    // Lines are translated one by one so the step structure (one step per line) survives
    private static List<String> lines(String text) {
        return text == null ? List.of() : Arrays.asList(text.split("\n", -1));
    }

    private static String joinLines(String original, List<String> translatedLines) {
        return original == null ? null : String.join("\n", translatedLines);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
