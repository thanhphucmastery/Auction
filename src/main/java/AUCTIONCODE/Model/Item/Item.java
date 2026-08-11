package AUCTIONCODE.Model.Item;

public abstract class Item {
    private String id;
    private String name;
    private String description;
    private String imagePath;
    public Item(String id, String name, String description) {
        this(id, name, description, "");
    }
    public Item(String id, String name, String description, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imagePath = imagePath == null ? "" : imagePath;
    }
    public String getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getImagePath() {
        return this.imagePath;
    }
}
