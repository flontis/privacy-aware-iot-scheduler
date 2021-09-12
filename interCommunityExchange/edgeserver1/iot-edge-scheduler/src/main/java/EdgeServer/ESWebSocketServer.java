package EdgeServer;


import EdgeServer.ForwardingUnit.ForwardingUnit;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.*;

public class ESWebSocketServer extends WebSocketServer {

    private static final KeyGenerator.Algorithm cryptoAlgo= KeyGenerator.Algorithm.AES;
    private static final int TCP_PORT = 4444;
    private Set<WebSocket> conns;
    private final UUID communityID;
    private JSONObject currentCapacityReport = new JSONObject();
    private final KeyGenerator keygen;
    private SecretKey sharedKey;
    private PriorityQueue<EdgeNodeEntry> edgeNodes = new PriorityQueue<>();
    private Map<String, String> uuidToIp = new HashMap<String, String>();
    private final String ownIP;
    ESWebSocketClientForICS webclient;
    protected ForwardingUnit fu;
    private final String icsIP = "10.0.0.55";

    {
        try {
            fu = new ForwardingUnit(9999);
            //fu.setTunnel("192.168.1.218:30000", "192.168.1.29:30001");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public ESWebSocketServer() throws URISyntaxException {
        super(new InetSocketAddress(TCP_PORT));
        System.out.println("Started Edge Server 3.0");
        conns = new HashSet<>();
        keygen = new KeyGenerator(cryptoAlgo);
        this.ownIP = getIPAddress(false);
        System.out.println("Edge Server: IP is " + this.ownIP);
        this.communityID = UUID.randomUUID();
        webclient = new ESWebSocketClientForICS(new URI("ws://"+this.icsIP+":5555"), "1", this.fu);
        webclient.connect();
    }

    private String encryptKey(String msgToEncrypt){
        try
        {

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sharedKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(msgToEncrypt.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    private String decryptKey(String msgToDecrypt){
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, sharedKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(msgToDecrypt)));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

    private void sendCurrentCommunityKey(WebSocket webSocket){
        webSocket.send("This is your Edge Server speaking");
        byte[] communityKey = keygen.getPubKey();
        byte[] sharedSecretBytes = keygen.getSharedSecret();
        sharedKey = new SecretKeySpec(sharedSecretBytes, 0, sharedSecretBytes.length, "AES");
        String messageToEncrypt = null;
        messageToEncrypt = new String(communityKey, StandardCharsets.ISO_8859_1);
        System.out.println("Original key at Edge Server: " + Arrays.toString(communityKey));
        String messagedComKey = encryptKey(messageToEncrypt);
        JSONObject communityKeyJSON = new JSONObject();
        communityKeyJSON.put("Type", "CommunityKeyMessage");
        communityKeyJSON.put("Algorithm", "AES");
        communityKeyJSON.put("Key", messagedComKey);
        webSocket.send(communityKeyJSON.toString());






    }


    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        conns.add(webSocket);
        //System.out.println("New connection from " + webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
        sendCurrentCommunityKey(webSocket);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
//        System.out.println("Edge Server received message: " + s);
        try {
            JSONObject message = new JSONObject(s);
            String messageType = (String) message.get("Type");
            switch (messageType){
                case "CapacityReport":
                    System.out.println("Edge Server: Received CapacityReport");

                    digestCapacityReport(message, webSocket);
                    break;
                case "EN Introduction":
                    System.out.println("Edge Server: Received Introduction");
                    digestIntroduction(message);
                    break;
                case "TunnelMessageFromICS":
                    System.out.println("Edge Server: Received TunnelMessageFromICS");
                    digestTunnelMessage(message);
                    break;
                default:
                    System.out.println("Edge Server: Received an unknown type of message: " + s);
            }
        }
        catch (JSONException e){
            e.printStackTrace();
            System.out.println("Edge Server: Received non-JSON message. --> " + s);
        }
    }

    private void digestTunnelMessage(JSONObject message) {
        String from = message.getString("From");
        String target = message.getString("Target");
        byte[] comKey = keygen.getPubKey();
        SecretKey sharedKey = new SecretKeySpec(comKey, 0, comKey.length, "AES");
        String decryptedUUID = "";
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, sharedKey);
            decryptedUUID = new String(cipher.doFinal(Base64.getDecoder().decode(target)));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }


        System.out.println("Setting Tunnel from " + from + " to " + decryptedUUID + " with ip " + this.uuidToIp.get(decryptedUUID));
        fu.setTunnel(from+":8000", this.uuidToIp.get(decryptedUUID)+":30000");
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        e.printStackTrace();
    }


    private void digestCapacityReport(JSONObject message, WebSocket webSocket){
        String uuid = message.getString("UUID");

        ////////////////////////UUID decryption//////////////////////////
        byte[] comKey = keygen.getPubKey();
        SecretKey sharedKey = new SecretKeySpec(comKey, 0, comKey.length, "AES");
        String decryptedUUID = "";
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, sharedKey);
            decryptedUUID = new String(cipher.doFinal(Base64.getDecoder().decode(uuid)));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        ///////////////////////Create Edge Node Entry///////////////////////////
        EdgeNodeEntry enEntry = new EdgeNodeEntry(message, decryptedUUID);
        edgeNodes.removeIf(entry -> Objects.equals(entry.UUID, enEntry.UUID));
        edgeNodes.add(enEntry);

        JSONObject host = message.getJSONObject("Host");
        String uuidencrypted = (String) message.get("UUID");
        currentCapacityReport.put(uuidencrypted, host.get("freeMem"));
        currentCapacityReport.put("communityID", this.communityID);
        currentCapacityReport.put("Type", "CapRep");
        System.out.println("CurrentCap4ICS: " + currentCapacityReport.toString(4));
        sendCapacityReportToICS(currentCapacityReport);
        if(enEntry.freeMem < (enEntry.maxMem * 0.00001)){
            System.out.println("Edge Server: Detected necessary rescheduling on EN " + enEntry.UUID);
            assert enEntry.containers.peek() != null;
            ADUContainer  adu = enEntry.containers.peek();
            long neededMemory = adu.memUsage;
            String reschedulingTargetUUID = getTargetForRescheduling(neededMemory);
            if (reschedulingTargetUUID.equals("")){
                System.out.println("Edge Server: Couldn't find appropriate local EN, rescheduling to ES-IP");
                int id = Integer.parseInt(adu.name.substring(adu.name.length() - 1));
                String entry = "10.0.0.65"+ ":" + (29999 + id);
                System.out.println("Set tunnel from Entry: " + entry + " to ICS at " + this.icsIP);
                fu.setTunnel(entry, this.icsIP);
                sendTunnelMessage(neededMemory, (29999 + id));
                sendRescheduleCommand(webSocket, adu.name);
            }
            else{
                String reschedulingTargetIP = this.uuidToIp.get(reschedulingTargetUUID);
                String aduToReschedule = adu.name;
                sendRescheduleCommand(webSocket, aduToReschedule);
                int id = Integer.parseInt(aduToReschedule.substring(aduToReschedule.length() - 1));
                String entry = "10.0.0.65"+ ":" + (29999 + id);
                System.out.println("Entry: " + entry + ", TargetIP: " + reschedulingTargetIP);
                fu.setTunnel(entry, "192.168.1.29:30001");
            }

            /*TODO wenn reschedulingTargetUUID leer ist, ist kein geeigner EN bekannt, dann IP ersetzen durch eigene IP
               damit an den ES-ADUM gescheduled wird
               erledigt, siehe oben. Testen!
             */

        }


    }

    private void sendTunnelMessage(long neededMemory, int id) {
        JSONObject tunnelMessage = new JSONObject();
        tunnelMessage.put("Type", "TunnelMessage");
        tunnelMessage.put("From", this.ownIP+ ":" + id);
        tunnelMessage.put("IP", this.ownIP);
        tunnelMessage.put("UUID", this.communityID);
        tunnelMessage.put("NecessaryMemory", neededMemory);
        this.webclient.sendTunnelMessage(tunnelMessage);
    }

    private void sendCapacityReportToICS(JSONObject currentCapacityReport) {
        currentCapacityReport.put("IP", this.ownIP);
        this.webclient.sendCapRep(currentCapacityReport);
    }

    public String getTargetForRescheduling(long neededMemory){
        PriorityQueue<EdgeNodeEntry> tmp = new PriorityQueue<>(Collections.reverseOrder());
        tmp.addAll(this.edgeNodes);
        while(true){
            if (!tmp.isEmpty()) {
                EdgeNodeEntry ene = tmp.poll();
                if (ene.freeMem > neededMemory * 1000000000){
                    return ene.UUID;
                }
            }
            else{
                System.out.println("No sufficient Edge Node found");
                return "";
            }

        }

    }

    public void sendRescheduleCommand(WebSocket webSocket, String aduToReschedule){
        System.out.println("Edge Server: Sending command to reschedule " + aduToReschedule);
        JSONObject json = new JSONObject();
        json.put("Type", "RescheduleCommand");
        json.put("ADU", aduToReschedule);
        json.put("Target", this.ownIP + ":9999");
        int id = Integer.parseInt(aduToReschedule.substring(aduToReschedule.length() - 1));
//        System.out.println("id: " + id + ", adu: " + aduToReschedule);

//        System.out.println("Edge Server: set Tunnel from " + entry + " to " + targetIP);

        webSocket.send(json.toString());
    }


    public static void main(String[] args) throws URISyntaxException {
        ESWebSocketServer server = new ESWebSocketServer();
        server.start();

    }

    public void digestIntroduction(JSONObject message){
        this.uuidToIp.put(message.getString("UUID"), message.getString("IP"));
    }

    private static String getIPAddress(boolean useIPv4) {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            return ip;
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }
}
