package Test;

import Model.Auction.AuctionRoom;
import Model.Item.Item;
import Model.Item.ItemFactory;
import Model.OtherInterface.AuctionStatus;
import Model.User.Player;
import Model.User.UserInformation;
import Network.RequestParser;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class SmokeTest {
    public static void main(String[] args) {
        String sellerSession = registerAndLogin(
                "seller",
                "secret123",
                "Seller One",
                "0123456789",
                "seller@example.com",
                "Ha Noi"
        );
        String buyerSession = registerAndLogin(
                "buyer",
                "secret123",
                "Buyer One",
                "0987654321",
                "buyer@example.com",
                "Da Nang"
        );
        String adminSession = registerAdminAndLogin(
                "admin",
                "secret123",
                "Admin One",
                "0123409876",
                "admin@example.com",
                "Ha Noi",
                "BIZ-001"
        );

        expectOk(RequestParser.handle("DEPOSIT:" + buyerSession + ":1000"));
        expectError(RequestParser.handle("ADMIN_GET_USERS:" + buyerSession));
        expectOk(RequestParser.handle("ADMIN_GET_USERS:" + adminSession));

        String createResponse = expectOk(RequestParser.handle(
                "CREATE_AUCTION:" + sellerSession + ":Laptop:100:10:5"
        ));
        String auctionId = createResponse.substring("OK:".length());

        expectOk(RequestParser.handle("JOIN_AUCTION:" + buyerSession + ":" + auctionId));
        expectOk(RequestParser.handle("BID:" + buyerSession + ":" + auctionId + ":150"));
        expectOk(RequestParser.handle("GET_AUCTIONS:" + sellerSession));
        expectOk(RequestParser.handle("END_AUCTION:" + sellerSession + ":" + auctionId));

        String adminEndedAuctionResponse = expectOk(RequestParser.handle(
                "CREATE_AUCTION:" + sellerSession + ":Tablet:100:10:5"
        ));
        String adminEndedAuctionId = adminEndedAuctionResponse.substring("OK:".length());
        expectOk(RequestParser.handle("END_AUCTION:" + adminSession + ":" + adminEndedAuctionId));
        testAutoExtendNearEndBid();

        System.out.println("SMOKE_TEST_OK");
    }

    private static void testAutoExtendNearEndBid() {
        Player seller = new Player(
                "auto_extend_seller",
                new UserInformation("Ha Noi", "0123451111", "seller-auto@example.com", "Seller Auto"),
                "hash",
                UUID.randomUUID().toString(),
                0
        );
        Player buyer = new Player(
                "auto_extend_buyer",
                new UserInformation("Da Nang", "0987651111", "buyer-auto@example.com", "Buyer Auto"),
                "hash",
                UUID.randomUUID().toString(),
                1000
        );
        Item item = ItemFactory.createArt(
                UUID.randomUUID().toString(),
                "Auto Extend Item",
                "Smoke test item",
                seller.getUserName(),
                LocalDateTime.now().getYear()
        );
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(5);
        AuctionRoom room = new AuctionRoom(
                UUID.randomUUID().toString(),
                seller.getUserId(),
                100,
                10,
                originalEndTime,
                item
        );

        room.startAuctionRoom();
        room.joinAuctionRoom(buyer);
        if (!room.placeBid(buyer, 150)) {
            throw new IllegalStateException("Expected auto-extend bid to be accepted");
        }
        long extendedBySeconds = ChronoUnit.SECONDS.between(originalEndTime, room.getEndTime());
        if (extendedBySeconds != AuctionRoom.AUTO_EXTEND_SECONDS) {
            throw new IllegalStateException("Expected auction to extend by "
                    + AuctionRoom.AUTO_EXTEND_SECONDS + " seconds, got " + extendedBySeconds);
        }
        if (room.getStatus() != AuctionStatus.EXTENDED) {
            throw new IllegalStateException("Expected auction status EXTENDED, got " + room.getStatus());
        }
    }

    private static String registerAndLogin(String username, String password, String fullName,
                                           String phone, String email, String address) {
        expectOk(RequestParser.handle("REGISTER:" + username + ":" + password + ":"
                + fullName + ":" + phone + ":" + email + ":" + address));
        String loginResponse = expectOk(RequestParser.handle("LOGIN:" + username + ":" + password));
        return loginResponse.substring("OK:".length());
    }

    private static String registerAdminAndLogin(String username, String password, String fullName,
                                                String phone, String email, String address,
                                                String businessCode) {
        expectOk(RequestParser.handle("REGISTER_ADMIN:" + username + ":" + password + ":"
                + fullName + ":" + phone + ":" + email + ":" + address + ":" + businessCode));
        String loginResponse = expectOk(RequestParser.handle("LOGIN:" + username + ":" + password));
        return loginResponse.substring("OK:".length());
    }

    private static String expectOk(String response) {
        if (response == null || !response.startsWith("OK")) {
            throw new IllegalStateException("Expected OK response, got: " + response);
        }
        return response;
    }

    private static String expectError(String response) {
        if (response == null || !response.startsWith("ERROR")) {
            throw new IllegalStateException("Expected ERROR response, got: " + response);
        }
        return response;
    }
}
