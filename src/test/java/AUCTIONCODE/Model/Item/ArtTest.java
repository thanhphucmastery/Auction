package AUCTIONCODE.Model.Item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtTest {

    @Test
    void testArtConstructorAndGetters() {
        Art art = new Art("A01", "Mona Lisa", "Bức tranh sơn dầu cổ điển", "Leonardo da Vinci", 1503);

        // Kiểm tra thuộc tính kế thừa từ Item
        assertEquals("A01", art.getId());
        assertEquals("Mona Lisa", art.getName());
        assertEquals("Bức tranh sơn dầu cổ điển", art.getDescription());

        // Kiểm tra thuộc tính riêng của lớp Art
        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getYearCreated());
    }
}