package Testclasses.WebSocketServer;

import EdgeServer.ForwardingUnit.ForwardingUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketServer extends WebSocketClient {
    protected ForwardingUnit fu;

    public WebSocketServer(URI serverURI, String uuid, ForwardingUnit fu) {
        super(serverURI);
        this.fu = fu;


    }
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("Connected to ICS");

    }

    @Override
    public void onMessage(String s) {
        System.out.println("Edge Server ICS-Client received message: " + s);
        JSONObject json = new JSONObject(s);
        if(json.getString("Type").equals("TunnelMessage")){
            String from = json.getString("From");
            String target = json.getString("Target");
            fu.setTunnel(from, target);
        }


    }

    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();

    }


    public static void main(String[] args) throws IOException, URISyntaxException {
        WebSocketServer ws = new WebSocketServer(new URI("ws://158.101.170.8:4444"), "", new ForwardingUnit(23456));
        ws.connect();
    }
}

