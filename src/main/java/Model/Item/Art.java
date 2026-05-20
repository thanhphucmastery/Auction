package Model.Item;

public class Art extends Item {
    private String artist;
    private int yearCreated;

    public Art(String id, String name, String description, String artist, int yearCreated) {
        super(id, name, description);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }
}
