import java.io.BufferedReader;
import java.io.EOFException;
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
        ServerSocket server;
        try {
            server = new ServerSocket(8080);
            System.out.println("Listening on 8080");
            Socket socket = server.accept();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            String key = null;

            while (!(line = reader.readLine()).isEmpty()) { // 1.
                System.out.println(line);
                if (line.startsWith("sec-websocket-key:")) {
                    key = line.substring(18).trim();
                }
            }

            String accept = getAccept(key);
            System.out.println(accept);

            String reply = "HTTP/1.1 101 Switching Protocols\r\n" + // 1.
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";

            out.write(reply.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String message = readMessage(in);

            System.out.println("Received from Client: " + message);

            sendMessage(out, generateNoise());
            System.out.println("Response sent");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void readAll(InputStream in, byte[] buf) throws IOException {
        int offset = 0;

        while (offset < buf.length) {
            int received = in.read(buf, offset, buf.length - offset);

            if (received == -1) {
                throw new EOFException("Connection closed");
            }

            offset += received;
        }
    }

    private static String readMessage(InputStream in) throws IOException {
        // 2.5.2
        int b1 = in.read(); // FIN + opcode
        int b2 = in.read(); // MASK + payload length indicator

        int payloadIndicator = b2 & 0b0111111;
        long payloadLength;

        if (payloadIndicator <= 125) {
            payloadLength = payloadIndicator;
        } else if (payloadIndicator == 126) {
            int b3 = in.read();
            int b4 = in.read();

            payloadLength = ((b3 & 0xFF) << 8) | (b4 & 0xFF);
            // We're doing & 0xFF because our bytes are being read into an integer type.
            // Java does this in an signed manner, but we don't want that so we use 0xFF to
            // turn e.g. FF FF FF FE into 00 00 00 FE
        } else {
            payloadLength = 0;

            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | (in.read() & 0xFF);
            }
        }

        byte[] mask = new byte[4]; // We ignore MASK bit because this message was Client -> Server so MASK == 1 is
                                   // required

        readAll(in, mask);

        byte[] payload = new byte[(int) payloadLength];
        readAll(in, payload);

        for (int i = 0; i < payload.length; i++) { // Mask is applied to every four byte frame
            payload[i] ^= mask[i % 4];
        }

        return new String(payload, StandardCharsets.UTF_8);
    }

    private static void sendMessage(OutputStream out, byte[] msg) throws IOException {
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

    private static void sendMessage(OutputStream out, String message) throws IOException {
        byte[] msg = message.getBytes(StandardCharsets.UTF_8);
        sendMessage(out, msg);
    }

    private static String getAccept(String key) { // 1.
        String plusRFC_ID = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        byte[] hash = md.digest(plusRFC_ID.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] generateNoise() {
        byte[] noise = new byte[128 * 1024];
        double t = 0;
        double dt = 2 * Math.PI * 1 / 48000;
        for (int i = 0; i < noise.length; i++) {
            noise[i] = (byte) Math.floor(Math.sin(t) * 127);
            t = (t + dt) % (Math.PI * 2);
        }

        return noise;
    }
}
