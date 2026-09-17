package family.cookbook.recipe;

public class Recipe {
    private String id;
    private String title;
    private String description;
    private String servings;
    private String prepTimeMinutes;
    private String cookTimeMinutes;
    private String instructions;
    private String imageUrl;
    private String createdBy;
    private String createdAt;

    public Recipe(String id, String title, String description, String servings, String prepTimeMinutes, String cookTimeMinutes, String instructions, String imageUrl, String createdBy, String createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.servings = servings;
        this.prepTimeMinutes = prepTimeMinutes;
        this.cookTimeMinutes = cookTimeMinutes;
        this.instructions = instructions;
        this.imageUrl = imageUrl;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServings() {
        return servings;
    }

    public void setServings(String servings) {
        this.servings = servings;
    }

    public String getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(String prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public String getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public void setCookTimeMinutes(String cookTimeMinutes) {
        this.cookTimeMinutes = cookTimeMinutes;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
