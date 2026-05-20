package Model.Item;

public class Electronics extends Item {
    private String brand;
    private int yearMade;
    private int warranty;

    public Electronics(String id, String name, String description, String brand, int yearMade, int warranty) {
        super(id, name, description);
        this.brand = brand;
        this.yearMade = yearMade;
        this.warranty = warranty;
    }

    public String getBrand() {
        return brand;
    }

    public int getYearMade() {
        return yearMade;
    }

    public int getWarranty() {
        return warranty;
    }
}