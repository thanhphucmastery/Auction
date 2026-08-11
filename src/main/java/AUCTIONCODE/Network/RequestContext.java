package AUCTIONCODE.Network;

import AUCTIONCODE.AuthModule.InMemorySessionManager;
import AUCTIONCODE.Database.AuctionDAO;
import AUCTIONCODE.Database.BidTransactionDAO;
import AUCTIONCODE.Database.ItemDAO;
import AUCTIONCODE.Database.USERDAO;
import AUCTIONCODE.Manager.AuctionManager;
import AUCTIONCODE.Manager.UserManager;

final class RequestContext {
    final InMemorySessionManager sessionManager = InMemorySessionManager.getInstance();
    final AuctionManager auctionManager = AuctionManager.getInstance();
    final UserManager userManager = UserManager.getInstance();
    final USERDAO userDAO = new USERDAO();
    final AuctionDAO auctionDAO = new AuctionDAO();
    final ItemDAO itemDAO = new ItemDAO();
    final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
}
