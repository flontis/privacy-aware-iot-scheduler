import ForwardingUnit.ForwardingUnit;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class InterCommunityWebServer extends WebSocketServer {
    private static final int TCP_PORT = 5555;
    private Set<WebSocket> conns;
    private ForwardingUnit fu;
    private HashMap<String, PriorityQueue<EdgeNodeEntry>> uuidToCapRep = new HashMap<>();
    private HashMap<String, String> UUIDToIP = new HashMap<>();
    private HashMap<String, String> IPToUUID = new HashMap<>();


    public InterCommunityWebServer() throws IOException {
        super(new InetSocketAddress(TCP_PORT));
        conns = new HashSet<>();
        System.out.println("Started ICS on port " + TCP_PORT);
        System.out.println("Alpha-Version");
        fu = new ForwardingUnit(8000);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        System.out.println("New connection from Edge Server");

    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        System.out.println("Received Message: " + s);
        JSONObject json = new JSONObject(s);
        String type = json.getString("Type");
        switch (type) {
            case "CapRep":
                String uuid = json.getString("communityID");
                String remoteIP = json.getString("IP");
                System.out.println("Mapping UUID " + uuid + " to IP " + remoteIP);
                this.UUIDToIP.put(uuid, remoteIP);
                this.IPToUUID.put(remoteIP, uuid);
                digestCapRep(json);

                break;
            case "TunnelMessage":
                EdgeNodeEntry targetEdgeNode = digestTunnelMessage(json);
                String targetEdgeNodeUUID = targetEdgeNode.UUID;
                System.out.println("DigestedTunnelMessage");
                String answerType = "TunnelMessageFromICS";
                String from = getIPAddress(true);
                String origin = json.getString("From");
                JSONObject answer = new JSONObject();
                answer.put("From", from);
                answer.put("Type", answerType);
                answer.put("Target", targetEdgeNodeUUID);
                //System.out.println("Trying to send TunnelMessage to ES");
                System.out.println("Adding tunnel from " + origin + " to " + UUIDToIP.get(targetEdgeNode.communityID));
                fu.setTunnel(origin, UUIDToIP.get(targetEdgeNode.communityID) + ":9999");
                try {
                    TunnelingWebSocket ws = new TunnelingWebSocket(new URI("ws://10.0.0.65:4444"), answer.toString());
                    ws.connect();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                //TODO irgendwie an den ES vom targetEN schicken
                break;
            default:
                System.out.println("Unknown type of message received.");
        }


    }

    private EdgeNodeEntry digestTunnelMessage(JSONObject json) {
//        System.out.println("Digesting Tunnel Message: " + json.toString(4));
        String remoteIP = json.getString("From");
        String remoteUUID = json.getString("UUID");
        System.out.println("Komme bis hier hin");

        long neededMem = (long) (int) json.get("NecessaryMemory");
        System.out.println("KeySet: " + uuidToCapRep.keySet());
        for (String communityID : uuidToCapRep.keySet()) {
            if (!communityID.equals(remoteUUID)) {
                System.out.println(remoteUUID + " equals not " + communityID);
                PriorityQueue<EdgeNodeEntry> tmp = uuidToCapRep.get(communityID);
                EdgeNodeEntry edgeNodeEntry = tmp.peek();
                assert edgeNodeEntry != null;
                if (edgeNodeEntry.freeMem >= neededMem) {
                    System.out.println("Found suitable EdgeNode with ID " + edgeNodeEntry.UUID);
                    System.out.println("Would ask Edge Server to introduce new tunnel from ICS to Edge Node " + edgeNodeEntry.UUID + "with ESIP " + edgeNodeEntry.ESIP);
                    return edgeNodeEntry;

                } else {
                    System.out.println("No EdgeNode with enough free Mem on community " + communityID);
                }
            } else {
                System.out.println(remoteIP + " equals " + communityID);
            }
        }
        return null;
    }


    private void digestCapRep(JSONObject json) {
        System.out.println("Started Digesting");
        String communityID = json.getString("communityID");
        PriorityQueue<EdgeNodeEntry> tmp = new PriorityQueue<>();

        for (String entry : json.keySet()) {
            if (!entry.equals("communityID") && !entry.equals("Type") && !entry.equals("IP")) {
                String uuid = entry;
                System.out.println(entry);

                long freeMem = (long) json.get(entry);

                EdgeNodeEntry edgeNodeEntry = new EdgeNodeEntry(uuid, freeMem, communityID, UUIDToIP.get(communityID));

                tmp.add(edgeNodeEntry);

            }
        }

        this.uuidToCapRep.put(communityID, tmp);
        this.uuidToCapRep.replace(communityID, tmp);
        System.out.println("Digested CapRep");
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    public static String getIPAddress(boolean useIPv4) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            return ip;
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) throws IOException {
        InterCommunityWebServer ics = null;
        try {
            ics = new InterCommunityWebServer();
            ics.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String ip = getIPAddress(false);
        //System.out.println(new URL(ip));

    }
}
