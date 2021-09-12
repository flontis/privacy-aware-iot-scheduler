import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TunnelingWebSocket extends WebSocketClient {
    private String message;
    public TunnelingWebSocket(URI serverURI, String message) {

        super(serverURI);
        this.message = message;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("Trying to send message to ES");
        send(message);
        System.out.println("Sent Message to ES");
        close();
    }

    @Override
    public void onMessage(String s) {

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.println("Closed connection to ES for setting Tunnel with message" + s + " and Boolean " + b);
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();

    }
}
