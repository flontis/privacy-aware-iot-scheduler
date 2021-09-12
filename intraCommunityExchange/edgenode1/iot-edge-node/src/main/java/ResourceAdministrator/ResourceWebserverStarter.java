package ResourceAdministrator;

import ResourceAdministrator.KeyStorage.KeyStorage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.Base64;


public class ResourceWebserverStarter extends WebSocketClient {
    KeyStorage keyStorage;
    String uuid;
    String ip;


    public ResourceWebserverStarter(URI serverURI, String uuid) {
        super(serverURI);
        keyStorage = new KeyStorage();

        this.uuid = uuid;
        System.out.println("Edge Node: Unencrypted id:" + this.uuid);
	System.out.println("Enhanced Version");


    }


    @Override
    public void onOpen(ServerHandshake serverHandshake) {

        ip = getIPAddress(false);

        System.out.println("New connection established, I am EN " + this.uuid + " with IP " + ip);
        // build introduction message
        JSONObject json = new JSONObject();
        json.put("Type", "EN Introduction");
        json.put("UUID", this.uuid);
        json.put("IP", ip);
        send(json.toString());

    }

    @Override
    public void onMessage(String s) {
        System.out.println("Edge Node received message: " + s);
        try {
            JSONObject message = new JSONObject(s);
            String messageType = (String) message.get("Type");
            switch (messageType){
                case "CommunityKeyMessage":
                    System.out.println("Edge Node: Received CommunityKeyMessage");
                    digestCommunityKeyMessage(message);
                    break;
                case "RescheduleCommand":
                    System.out.println("Edge Node: Received RescheduleCommand");
                    digestRescheduleCommand(message);
                    break;
                default:
                    System.out.println("Edge Node: Received an unknown type of message.");
            }
        }
        catch (JSONException e){
            System.out.println("Edge Node: Received non-JSON message.");
        }




    }

    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {
    e.printStackTrace();
    }



    public void sendReport(JSONObject capacityReport){
        String uuid = (String) capacityReport.get("UUID");

        try {
            byte[] comKeyBytes = keyStorage.getEntry("community-key").getEncoded();
            SecretKey comKey = new SecretKeySpec(comKeyBytes, 0, comKeyBytes.length, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, comKey);
            uuid = Base64.getEncoder().encodeToString(cipher.doFinal(uuid.getBytes(StandardCharsets.UTF_8)));
            capacityReport.put("UUID", uuid);

        } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        capacityReport.put("Type", "CapacityReport");
        System.out.println("Capacity Report:" + capacityReport.toString(4));
        send(capacityReport.toString());
    }

    private void digestCommunityKeyMessage(JSONObject message){
        try {
            String communityKeyBytes = decryptKey((String) message.get("Key"));
            assert communityKeyBytes != null;
            byte[] comBytes = communityKeyBytes.getBytes(StandardCharsets.ISO_8859_1);
            keyStorage.storeKey(comBytes, "community-key");
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }

    }

    private String decryptKey(String msgToDecrypt) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        byte[] sharedSecretBytes = keyStorage.getEntry("shared-key").getEncoded();
        SecretKey sharedKey = new SecretKeySpec(sharedSecretBytes, 0, sharedSecretBytes.length, "AES");
        //System.out.println("Edge Node: shared key bytes: " + Arrays.toString(sharedSecretBytes));
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

    public static String getIPAddress(boolean useIPv4) {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            return ip;
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void digestRescheduleCommand(JSONObject message){
        String adu = message.getString("ADU");
       // String targetIP = message.getString("Target");
	String targetIP = "10.0.0.94";
        int id = Integer.parseInt(adu.substring(adu.length() - 1));
        int startADUport =  29999 + id;
        System.out.println("Edge Node - Reschedule Command from " + adu + ":" + startADUport + " to " + targetIP);

        rescheduleADU(startADUport, 40000);
        System.out.println("Edge Node: Executed Rescheduling Command");

    }



    private void rescheduleADU(int startADUport, int anrePort){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpPost httppost = new HttpPost("http://10.0.0.64:" + startADUport + "/adu/reschedule");

            JSONObject json = new JSONObject();
            json.put("relatedSensorID", 1);
            json.put("relatedSensorAddress", "http://10.0.0.64"+":20000");
            json.put("algorithm", "birch");
            json.put("ID", 2);
            json.put("adumAddress", "http://" + "10.0.0.171" + ":" + "9999");
            json.put("relatedSensorMeta", "");
            json.put("anomalyReceiver", Arrays.asList("http://10.0.0.64:" + anrePort));
            json.put("reschedulingTimeout", 0);

            httppost.setEntity(new StringEntity(json.toString()));
            System.out.println(json.toString(4));
            System.out.println("Posting Reschedule Request to 10.0.0.171:9999");
            CloseableHttpResponse response = httpclient.execute(httppost);
            System.out.println("Executed and returned from rescheduling request");
            System.out.println("----------------------------------------");
            System.out.println(response.getStatusLine());

            // Get hold of the response entity
            final HttpEntity entity = response.getEntity();

            // If the response does not enclose an entity, there is no need
            // to bother about connection release
            if (entity != null) {
                try (final InputStream inStream = entity.getContent()) {
                    String encoding = "UTF-8";

                    String body = IOUtils.toString(inStream, encoding);
                    // do something useful with the response
                    System.out.println(body);
                } catch (final IOException ex) {
                    // In case of an IOException the connection will be released
                    // back to the connection manager automatically
                    throw ex;
                }
            }
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
