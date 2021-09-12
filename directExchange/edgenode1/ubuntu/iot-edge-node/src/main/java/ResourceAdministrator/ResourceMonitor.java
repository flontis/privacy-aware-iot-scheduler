package ResourceAdministrator;


import ResourceAdministrator.requester.DockerCPURequester;
import ResourceAdministrator.requester.DockerMemRequester;
import ResourceAdministrator.requester.HostResourceRequester;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class ResourceMonitor {

    //TODO Kommentare, Kommentare, Kommentare. Hier und in den Requesters
    //TODO ask docker for the sensor ID and ADU ID and so on
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    private int sensor = 20000;
    public static String bytesToHex(byte[] bytes) {

        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public ResourceMonitor() {
    }


private static void spawnADU(int port){
    try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
        final HttpPost httppost = new HttpPost("http://10.0.0.64:" + 20000 + "/adu/spawn");

        JSONObject json = new JSONObject();
        json.put("relatedSensorID", 1);
        json.put("relatedSensorAddress", "http://10.0.0.64:20000");
        json.put("algorithm", "birch");
        json.put("ID", 2);
        json.put("adumAddress", "http://10.0.0.64:" + port);
        json.put("anomalyReceiver", Arrays.asList("http://10.0.0.64:40000"));

        httppost.setEntity(new StringEntity(json.toString()));

        CloseableHttpResponse response = httpclient.execute(httppost);
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

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        System.out.println("Starting Resource Monitor");
        // create all necessary instances
        // put requests in different threads to ensure they are made as close as possible together in time
        DockerMemRequester memReq = new DockerMemRequester();
        DockerCPURequester CPUReq = new DockerCPURequester();
        HostResourceRequester hostReq = new HostResourceRequester();
//        spawnADU(30000);
        spawnADU(30001);
        MessageDigest salt;
        String uuid= "";
        try {
            salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
             uuid= bytesToHex(salt.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        ResourceWebserverStarter communicationChannel = new ResourceWebserverStarter(new URI("ws://" + "158.101.170.8" +  ":4444"), uuid);
        communicationChannel.connect();




        boolean development = false;
        while(!development) {
            // request respective resources
            JSONObject memoryUsagePerContainer = memReq.getMemoryUsageOfAllContainers();
            JSONObject cpuUsagePerContainer = CPUReq.getCPUUsageOfAllContainers();
            JSONObject hostResources = hostReq.getHostResources();

            // combine JSONs containing resources
            JSONObject combined = new JSONObject();
            combined.put("UUID", uuid);
            combined.put("Host", hostResources);
            combined.put("CPU", cpuUsagePerContainer);
            combined.put("Memory", memoryUsagePerContainer);
            System.out.println(combined);
            communicationChannel.sendReport(combined);
            Thread.sleep(10000);


            //System.out.println(combined.toString(4));
        }
        boolean done= false;
        while(development){
            if(!done){
                JSONObject memoryUsagePerContainer = memReq.getMemoryUsageOfAllContainers();
                JSONObject cpuUsagePerContainer = CPUReq.getCPUUsageOfAllContainers();
                JSONObject hostResources = hostReq.getHostResources();

                // combine JSONs containing resources
                JSONObject combined = new JSONObject();
                combined.put("Host", hostResources);
                combined.put("CPU", cpuUsagePerContainer);
                combined.put("Memory", memoryUsagePerContainer);
                communicationChannel.sendReport(combined);
                done = true;
            }
            sleep(2000);
            System.out.println("Waiting...");
        }
        //return combined;
    }

}
