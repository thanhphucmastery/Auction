package AUCTIONCODE.Model.Item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ItemFactoryTest {

    @Test
    void createArtBuildsArtWithExpectedFields() {
        Item item = ItemFactory.createArt("art-1", "Painting", "Oil on canvas", "Nguyen A", 2024);

        Art art = assertInstanceOf(Art.class, item);
        assertEquals("art-1", art.getId());
        assertEquals("Painting", art.getName());
        assertEquals("Oil on canvas", art.getDescription());
        assertEquals("Nguyen A", art.getArtist());
        assertEquals(2024, art.getYearCreated());
    }

    @Test
    void createElectronicsBuildsElectronicsWithExpectedFields() {
        Item item = ItemFactory.createElectronics("el-1", "Laptop", "M3 laptop", "Apple", 2025, 24);

        Electronics electronics = assertInstanceOf(Electronics.class, item);
        assertEquals("el-1", electronics.getId());
        assertEquals("Laptop", electronics.getName());
        assertEquals("M3 laptop", electronics.getDescription());
        assertEquals("Apple", electronics.getBrand());
        assertEquals(2025, electronics.getYearMade());
        assertEquals(24, electronics.getWarranty());
    }

    @Test
    void createVehicleBuildsVehicleWithExpectedFields() {
        Item item = ItemFactory.createVehicle("vh-1", "Car", "Used sedan", 12000.5, "Civic", 2022);

        Vehicle vehicle = assertInstanceOf(Vehicle.class, item);
        assertEquals("vh-1", vehicle.getId());
        assertEquals("Car", vehicle.getName());
        assertEquals("Used sedan", vehicle.getDescription());
        assertEquals(12000.5, vehicle.getMileage());
        assertEquals("Civic", vehicle.getModel());
        assertEquals(2022, vehicle.getYearMade());
    }
}
