package AUCTIONCODE.Network;

import AUCTIONCODE.Model.Item.Item;
import AUCTIONCODE.Model.User.User;

import java.util.UUID;
import java.util.stream.Collectors;

final class ItemRequestHandler {
    private final RequestContext context;

    ItemRequestHandler(RequestContext context) {
        this.context = context;
    }

    String handleAddItem(String[] parts) {
        RequestSupport.requireLength(parts, 8, "ADD_ITEM:sessionId:type:name:description:extra1:extra2:extra3:imagePath");
        User owner = RequestSupport.requireUser(context, parts[1]);
        Item item = RequestSupport.createItemFromParts(
                UUID.randomUUID().toString(),
                parts[2],
                parts[3],
                parts[4],
                parts[5],
                parts[6],
                parts[7],
                parts.length >= 9 ? parts[8] : ""
        );
        context.itemDAO.saveInventoryItem(item, owner.getUserId());
        return "OK:" + item.getId();
    }

    String handleGetMyItems(String[] parts) {
        RequestSupport.requireLength(parts, 2, "GET_MY_ITEMS:sessionId");
        User owner = RequestSupport.requireUser(context, parts[1]);
        String payload = context.itemDAO.findAvailableByOwner(owner.getUserId()).stream()
                .map(item -> RequestSupport.itemPayload(context, item))
                .collect(Collectors.joining(";"));
        return "OK:" + payload;
    }

    String handleUpdateItem(String[] parts) {
        RequestSupport.requireLength(parts, 9, "UPDATE_ITEM:sessionId:itemId:type:name:description:extra1:extra2:extra3:imagePath");
        User owner = RequestSupport.requireUser(context, parts[1]);
        Item current = context.itemDAO.findAvailableByOwner(parts[2], owner.getUserId());
        if (current == null) {
            return "ERROR:Item not found or already in auction";
        }
        Item item = RequestSupport.createItemFromParts(
                parts[2],
                parts[3],
                parts[4],
                parts[5],
                parts[6],
                parts[7],
                parts[8],
                parts.length >= 10 ? parts[9] : ""
        );
        context.itemDAO.updateInventoryItem(item, owner.getUserId());
        return "OK:Item updated";
    }

    String handleDeleteItem(String[] parts) {
        RequestSupport.requireLength(parts, 3, "DELETE_ITEM:sessionId:itemId");
        User owner = RequestSupport.requireUser(context, parts[1]);
        return context.itemDAO.deleteAvailable(parts[2], owner.getUserId())
                ? "OK:Item deleted"
                : "ERROR:Item not found or already in auction";
    }
}
