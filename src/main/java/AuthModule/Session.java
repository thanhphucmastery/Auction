package AuthModule;

public class Session {
    private String sessionId;
    private String userId;
    private long expireTime;

    public Session(String sessionId, String userId, long expireTime) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.expireTime = expireTime;
    }
    public void refresh(long timeout) {
        this.expireTime = System.currentTimeMillis() + timeout;
    }
    public long getExpireTime(){
        return expireTime;
    }

    public void setExpireTime(long time){
        this.expireTime=time;

    }
    public String getUserId(){
        return userId;
    }
}

