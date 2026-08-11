package AUCTIONCODE.Network;

@FunctionalInterface
interface RequestHandler {
    String handle(String[] parts);
}
