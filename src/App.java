import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;

/**
 * @formatter:off
 * Check these page for information on some of the stuff happening in this code
 * 1. https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Sec-WebSocket-Accept
 * 2. https://www.ietf.org/rfc/rfc6455.txt
 * 
 * Matthew Weyer
 * @formatter:on
 */
public class App {
    public static void main(String[] args) {
        try {
            ServerConfig config = parseConfig("settings.yaml");
            System.out.println("Server URL: " + config.serverUrl);
            
            String path = config.serverUrl;
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String url = path + "/rest/getArtists?u=" + config.username + "&p=" + config.password + "&v=1.16.1&c=maudio&f=json";
            
            System.out.println("Fetching artists...");
            String response = sendGetRequest(url);
            System.out.println("Response:\n" + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String sendGetRequest(String urlStr) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        String host = url.getHost();
        boolean isHttps = "https".equalsIgnoreCase(url.getProtocol());
        int port = url.getPort() != -1 ? url.getPort() : (url.getDefaultPort() != -1 ? url.getDefaultPort() : (isHttps ? 443 : 80));
        String path = url.getFile();
        if (path.isEmpty()) path = "/";

        Socket socket = isHttps ? SSLSocketFactory.getDefault().createSocket(host, port) : new Socket(host, port);
        try (Socket s = socket) {
            OutputStream out = s.getOutputStream();
            String request = "GET " + path + " HTTP/1.1\r\n" +
                             "Host: " + host + "\r\n" +
                             "Connection: close\r\n\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            return readHttpResponse(s.getInputStream());
        }
    }

    public static String sendPostRequest(String urlStr, String payload) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        String host = url.getHost();
        boolean isHttps = "https".equalsIgnoreCase(url.getProtocol());
        int port = url.getPort() != -1 ? url.getPort() : (url.getDefaultPort() != -1 ? url.getDefaultPort() : (isHttps ? 443 : 80));
        String path = url.getFile();
        if (path.isEmpty()) path = "/";

        byte[] body = payload.getBytes(StandardCharsets.UTF_8);

        Socket socket = isHttps ? SSLSocketFactory.getDefault().createSocket(host, port) : new Socket(host, port);
        try (Socket s = socket) {
            OutputStream out = s.getOutputStream();
            String request = "POST " + path + " HTTP/1.1\r\n" +
                             "Host: " + host + "\r\n" +
                             "Content-Length: " + body.length + "\r\n" +
                             "Content-Type: application/x-www-form-urlencoded\r\n" +
                             "Connection: close\r\n\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();

            return readHttpResponse(s.getInputStream());
        }
    }

    private static String readHttpResponse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        boolean inBody = false;
        while ((line = reader.readLine()) != null) {
            if (!inBody) {
                if (line.isEmpty()) {
                    inBody = true;
                }
            } else {
                response.append(line).append("\n");
            }
        }
        return response.toString();
    }

    public static class ServerConfig {
        public String serverUrl;
        public String username;
        public String password;
    }

    public static ServerConfig parseConfig(String filePath) throws IOException {
        ServerConfig config = new ServerConfig();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    switch (key) {
                        case "server_url":
                        case "serverUrl":
                        case "url":
                            config.serverUrl = value;
                            break;
                        case "username":
                        case "user":
                            config.username = value;
                            break;
                        case "password":
                        case "pass":
                            config.password = value;
                            break;
                    }
                }
            }
        }
        return config;
    }
}
