package Model.Item;

public class Vehicle extends Item {
    private double mileage;
    private String model;
    private int yearMade;

    public Vehicle(String id, String name, String description, double mileage, String model, int year) {
        super(id, name, description);
        this.mileage = mileage;
        this.model = model;
        this.yearMade = year;
    }

    public double getMileage() {
        return mileage;
    }

    public String getModel() {
        return model;
    }

    public int getYearMade() {
        return yearMade;
    }
}
