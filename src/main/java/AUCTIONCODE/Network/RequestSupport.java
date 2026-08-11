package AUCTIONCODE.Network;

import AUCTIONCODE.Model.Auction.AuctionRoom;
import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.Item.ItemFactory;
import AUCTIONCODE.Model.User.Player;
import AUCTIONCODE.Model.User.User;

import java.time.LocalDateTime;

final class RequestSupport {
    private RequestSupport() {
    }

    static void requireLength(String[] parts, int expected, String format) {
        if (parts.length < expected) {
            throw new IllegalArgumentException("Invalid request format. Expected " + format);
        }
    }

    static User requireUser(RequestContext context, String sessionId) {
        if (!context.sessionManager.isValidSession(sessionId)) {
            throw new IllegalArgumentException("Invalid session");
        }
        User user = context.userManager.getUser(context.sessionManager.getUserId(sessionId));
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    static Player requirePlayer(RequestContext context, String sessionId) {
        User user = requireUser(context, sessionId);
        if (!(user instanceof Player player)) {
            throw new IllegalArgumentException("Only player accounts can do this action");
        }
        return player;
    }

    static User requireAdmin(RequestContext context, String sessionId) {
        User user = requireUser(context, sessionId);
        if (!"Admin".equalsIgnoreCase(user.getUserRole())) {
            throw new IllegalArgumentException("Only admin accounts can do this action");
        }
        return user;
    }

    static AuctionRoom requireAuction(RequestContext context, String auctionId) {
        AuctionRoom room = context.auctionManager.getAuction(auctionId);
        if (room == null) {
            throw new IllegalArgumentException("Auction not found");
        }
        return room;
    }

    static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ")
                .replace(";", " ")
                .replace(",", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    static String displayUserName(RequestContext context, String userId) {
        if (userId == null || userId.isBlank()) {
            return "";
        }
        User user = context.userManager.getUser(userId);
        if (user == null) {
            return userId;
        }
        String fullName = user.getUserInformation() == null ? "" : user.getUserInformation().getFullName();
        return fullName == null || fullName.isBlank() ? user.getUserName() : fullName;
    }

    static String sellerUserName(RequestContext context, String userId) {
        User user = context.userManager.getUser(userId);
        return user == null ? "" : user.getUserName();
    }

    static String sellerDisplayName(RequestContext context, String userId) {
        User user = context.userManager.getUser(userId);
        if (user == null || user.getUserInformation() == null) {
            return "";
        }
        return user.getUserInformation().getFullName();
    }

    static Item createItemFromParts(String itemId, String type, String name, String description,
                                    String extra1, String extra2, String extra3, String imagePath) {
        int currentYear = LocalDateTime.now().getYear();
        return switch (type) {
            case "Art" -> ItemFactory.createArt(
                    itemId,
                    name,
                    description,
                    imagePath,
                    extra1 == null || extra1.isBlank() ? "Unknown" : extra1,
                    parseOptionalInt(extra2, currentYear)
            );
            case "Electronics" -> ItemFactory.createElectronics(
                    itemId,
                    name,
                    description,
                    imagePath,
                    extra1 == null || extra1.isBlank() ? "Unknown" : extra1,
                    parseOptionalInt(extra2, currentYear),
                    parseOptionalInt(extra3, 0)
            );
            case "Vehicle" -> ItemFactory.createVehicle(
                    itemId,
                    name,
                    description,
                    imagePath,
                    parseOptionalDouble(extra3, 0),
                    extra1 == null || extra1.isBlank() ? "Unknown" : extra1,
                    parseOptionalInt(extra2, currentYear)
            );
            default -> throw new IllegalArgumentException("Invalid item type");
        };
    }

    static String itemPayload(RequestContext context, Item item) {
        String type = context.itemDAO.getType(item);
        String extra1 = "";
        String extra2 = "";
        String extra3 = "";
        if (item instanceof AUCTIONCODE.Model.Item.Art art) {
            extra1 = art.getArtist();
            extra2 = String.valueOf(art.getYearCreated());
        } else if (item instanceof AUCTIONCODE.Model.Item.Electronics electronics) {
            extra1 = electronics.getBrand();
            extra2 = String.valueOf(electronics.getYearMade());
            extra3 = String.valueOf(electronics.getWarranty());
        } else if (item instanceof AUCTIONCODE.Model.Item.Vehicle vehicle) {
            extra1 = vehicle.getModel();
            extra2 = String.valueOf(vehicle.getYearMade());
            extra3 = String.valueOf(vehicle.getMileage());
        }
        return item.getId() + "|" + type + "|" + safe(item.getName()) + "|"
                + safe(item.getDescription()) + "|" + safe(extra1) + "|"
                + safe(extra2) + "|" + safe(extra3) + "|" + safe(item.getImagePath());
    }

    static String itemAttributes(Item item) {
        if (item instanceof AUCTIONCODE.Model.Item.Art art) {
            return "Tác giả: " + art.getArtist() + " | Năm sáng tác: " + art.getYearCreated();
        }
        if (item instanceof AUCTIONCODE.Model.Item.Electronics electronics) {
            return "Thương hiệu: " + electronics.getBrand() + " | Năm sản xuất: "
                    + electronics.getYearMade() + " | Bảo hành: " + electronics.getWarranty() + " tháng";
        }
        if (item instanceof AUCTIONCODE.Model.Item.Vehicle vehicle) {
            return "Dòng xe: " + vehicle.getModel() + " | Năm sản xuất: "
                    + vehicle.getYearMade() + " | Quãng đường: " + vehicle.getMileage() + " km";
        }
        return "";
    }

    static String transactionPayload(String time, String type, String auctionId, String itemName,
                                     String note, double amount, String counterparty) {
        return safe(time) + "|" + safe(type) + "|" + safe(auctionId) + "|" + itemName + "|"
                + safe(note) + "|" + amount + "|" + safe(counterparty);
    }

    static double parsePositiveDouble(String value, String fieldName) {
        double parsed = Double.parseDouble(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return parsed;
    }

    static long parsePositiveLong(String value, String fieldName) {
        long parsed = Long.parseLong(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return parsed;
    }

    static long parseNonNegativeLong(String value, String fieldName) {
        long parsed = Long.parseLong(value);
        if (parsed < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return parsed;
    }

    private static int parseOptionalInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private static double parseOptionalDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(value);
    }
}
