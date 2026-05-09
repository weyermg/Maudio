package maudio;
import java.util.ArrayList;
import java.util.List;


public class SubsonicConnection {
    private String baseUrl;
    private String username;
    private String password;

    public SubsonicConnection(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    //TODO: implement this
    public List<MusicFolder> getMusicFolders() {
        try{
            String requestUrl = baseUrl + "/rest/getMusicDirectory.view?u=" + username + "&p=" + password
                    + "&v=1.16.1&c=maudio&f=json";
            String response = HttpUtils.sendGetRequest(requestUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        

        return new ArrayList<>();
    }

    //TODO: implement this
    public List<Song> getSongs(MusicFolder folder) {
        return new ArrayList<>();
    }
}

