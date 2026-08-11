package AUCTIONCODE.Model.Item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElectronicsTest {

    @Test
    void testElectronicsConstructorAndGetters() {
        Electronics laptop = new Electronics("E01", "MacBook Pro", "M3 Max Chip", "Apple", 2024, 12);

        // Kiểm tra các thuộc tính chung kế thừa từ Item
        assertEquals("E01", laptop.getId());
        assertEquals("MacBook Pro", laptop.getName());
        assertEquals("M3 Max Chip", laptop.getDescription());

        // Kiểm tra các thuộc tính riêng của thiết bị điện tử
        assertEquals("Apple", laptop.getBrand());
        assertEquals(2024, laptop.getYearMade());
        assertEquals(12, laptop.getWarranty());
    }
}