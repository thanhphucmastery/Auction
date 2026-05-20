package Model.Item;

public class ItemFactory {

    public static Art createArt(String id, String name, String description,
                                String artist, int yearCreated) {
        return new Art(id, name, description, artist, yearCreated);
    }

    public static Electronics createElectronics(String id, String name, String description,
                                                String brand, int yearMade, int warranty) {
        return new Electronics(id, name, description, brand, yearMade, warranty);
    }

    public static Vehicle createVehicle(String id, String name, String description,
                                        double mileage, String model, int yearMade) {
        return new Vehicle(id, name, description, mileage, model, yearMade);
    }
}
