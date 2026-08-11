package AUCTIONCODE.Manager;

import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.Item.ItemFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager auctionManager;
    private Item sampleItem;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        auctionManager.clear(); // Reset dữ liệu của map để tránh ảnh hưởng giữa các test case
        sampleItem = ItemFactory.createArt("ART_01", "Tranh Dong Ho", "Tranh dan gian", "Unknown", 1990);
    }

    @Test
    void testSingletonInstance_ShouldReturnSameInstance() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    void testAddAndGetAuction_SuccessfulScenario() {
        String roomId = "ROOM_101";
        LocalDateTime endTime = LocalDateTime.now().plusHours(2);
        AuctionRoom room = new AuctionRoom(roomId, "SELLER_ID", 1000.0, 50.0, endTime, sampleItem, null);

        auctionManager.addAuction(room);

        AuctionRoom retrievedRoom = auctionManager.getAuction(roomId);
        assertNotNull(retrievedRoom);
        assertSame(room, retrievedRoom);
        assertEquals(1000.0, retrievedRoom.getCurrentPrice());
    }

    @Test
    void testGetAuction_WithNonExistentId_ShouldReturnNull() {
        assertNull(auctionManager.getAuction("ROOM_GHOST"));
    }

    @Test
    void testGetAllAuctions_ShouldReturnAllRegisteredRooms() {
        assertTrue(auctionManager.getAllAuctions().isEmpty());

        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        AuctionRoom room1 = new AuctionRoom("R1", "SELLER_A", 500.0, 10.0, endTime, sampleItem, null);
        AuctionRoom room2 = new AuctionRoom("R2", "SELLER_B", 600.0, 20.0, endTime, sampleItem, null);

        auctionManager.addAuction(room1);
        auctionManager.addAuction(room2);

        List<AuctionRoom> allAuctions = auctionManager.getAllAuctions();
        assertEquals(2, allAuctions.size());
        assertTrue(allAuctions.contains(room1));
        assertTrue(allAuctions.contains(room2));
    }

    @Test
    void testRemoveAuction_ShouldDeleteRoomFromMap() {
        String roomId = "ROOM_TO_DELETE";
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        AuctionRoom room = new AuctionRoom(roomId, "SELLER_A", 500.0, 10.0, endTime, sampleItem, null);

        auctionManager.addAuction(room);
        assertNotNull(auctionManager.getAuction(roomId));

        auctionManager.removeAuction(roomId);

        assertNull(auctionManager.getAuction(roomId));
        assertTrue(auctionManager.getAllAuctions().isEmpty());
    }

    @Test
    void testClear_ShouldRemoveAllAuctions() {
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        auctionManager.addAuction(new AuctionRoom("R1", "S", 10.0, 1.0, endTime, sampleItem, null));
        auctionManager.addAuction(new AuctionRoom("R2", "S", 20.0, 2.0, endTime, sampleItem, null));
        assertEquals(2, auctionManager.getAllAuctions().size());

        auctionManager.clear();

        assertTrue(auctionManager.getAllAuctions().isEmpty());
    }
}