
public class Song {
    private String id;
    private String parent;
    private boolean isDir;
    private String title;
    private String artist;
    private String coverArt;

    public Song(String id, String parent, boolean isDir, String title, String artist, String coverArt) {
        this.id = id;
        this.parent = parent;
        this.isDir = isDir;
        this.title = title;
        this.artist = artist;
        this.coverArt = coverArt;
    }

    public String getId() {
        return id;
    }

    public String getParent() {
        return parent;
    }

    public boolean isDir() {
        return isDir;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getCoverArt() {
        return coverArt;
    }
}
