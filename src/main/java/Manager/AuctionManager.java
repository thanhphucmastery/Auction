package Manager;


import Model.Auction.AuctionRoom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static final Map<String, AuctionRoom> auctionMap = new ConcurrentHashMap<>();
    private static AuctionManager instance;
    private  AuctionManager(){};
    public static AuctionManager getInstance(){
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        } return  instance;
    }
    public List<AuctionRoom> getAllAuctions() {
        return new ArrayList<>(auctionMap.values());
    }


    public void addAuction(AuctionRoom auction){
        auctionMap.put(auction.getId(), auction);
    }

    public AuctionRoom getAuction(String AuctionId){
        return auctionMap.get(AuctionId);
    }


}
