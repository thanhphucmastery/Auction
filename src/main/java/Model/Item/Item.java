package Model.Item;

public abstract class Item {
    private String id;
    private String name;
    private String description;
    public Item(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}
