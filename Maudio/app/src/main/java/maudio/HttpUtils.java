package maudio;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLSocketFactory;

public class HttpUtils {
    public static String sendGetRequest(String urlStr) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        String host = url.getHost();
        boolean isHttps = "https".equalsIgnoreCase(url.getProtocol());
        int port = url.getPort() != -1 ? url.getPort()
                : (url.getDefaultPort() != -1 ? url.getDefaultPort() : (isHttps ? 443 : 80));
        String path = url.getFile();
        if (path.isEmpty())
            path = "/";

        Socket socket = isHttps ? SSLSocketFactory.getDefault().createSocket(host, port) : new Socket(host, port);
        try (Socket s = socket) {
            OutputStream out = s.getOutputStream();
            String request = "GET " + path + " HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            return HttpUtils.readHttpResponse(s.getInputStream());
        }
    }

    public static String sendPostRequest(String urlStr, String payload) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        String host = url.getHost();
        boolean isHttps = "https".equalsIgnoreCase(url.getProtocol());
        int port = url.getPort() != -1 ? url.getPort()
                : (url.getDefaultPort() != -1 ? url.getDefaultPort() : (isHttps ? 443 : 80));
        String path = url.getFile();
        if (path.isEmpty())
            path = "/";

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

    public static void sendMessage(OutputStream out, byte[] msg) throws IOException {
        out.write(0b10000001);

        if (msg.length <= 125) {
            out.write(msg.length);
        } else if (msg.length <= 65535) {
            out.write(126);
            out.write((msg.length >> 8) & 0xFF);
            out.write(msg.length & 0xFF);
        } else {
            out.write(127);
            long len = msg.length;

            for (int i = 7; i >= 0; i--) {
                out.write((int) ((len >> (8 * i)) & 0xFF));
            }
        }

        out.write(msg);
        out.flush();
    }

    public static void sendMessage(OutputStream out, String message) throws IOException {
        byte[] msg = message.getBytes(StandardCharsets.UTF_8);
        sendMessage(out, msg);
    }

}