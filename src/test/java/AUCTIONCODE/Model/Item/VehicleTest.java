package AUCTIONCODE.Model.Item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    @Test
    void testVehicleConstructorAndGetters() {
        Vehicle car = new Vehicle("V01", "Porsche 911", "Xe thể thao màu đỏ", 500.5, "911 Carrera", 2023);

        // Kiểm tra các thuộc tính chung kế thừa từ Item
        assertEquals("V01", car.getId());
        assertEquals("Porsche 911", car.getName());
        assertEquals("Xe thể thao màu đỏ", car.getDescription());

        // Kiểm tra các thuộc tính riêng của phương tiện
        assertEquals(500.5, car.getMileage());
        assertEquals("911 Carrera", car.getModel());
        assertEquals(2023, car.getYearMade());
    }
}