package EdgeServer;

import EdgeServer.ForwardingUnit.ForwardingUnit;
import ResourceAdministrator.KeyStorage.KeyStorage;
import ResourceAdministrator.ResourceWebserverStarter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLOutput;

public class ESWebSocketClientForICS extends WebSocketClient {
    protected ForwardingUnit fu;

    public ESWebSocketClientForICS(URI serverURI, String uuid, ForwardingUnit fu) {
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

    }



    public void sendCapRep(JSONObject currentCapacityReport) {
        this.send(String.valueOf(currentCapacityReport));
        System.out.println("Edge Server: Sent CapRep to ICS");
    }

    public void sendTunnelMessage(JSONObject tunnelMessage) {
        this.send(String.valueOf(tunnelMessage));
        System.out.println("Edge Server: Sent tunnelMessage to ICS");
    }
}
