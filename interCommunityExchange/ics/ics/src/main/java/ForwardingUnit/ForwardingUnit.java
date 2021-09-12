package ForwardingUnit;


import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ForwardingUnit {

    private Map<String, String> senderReceiverMapping = new HashMap<String, String>();
    private Map<String, String> receiverSensorMapping = new HashMap<String, String>();
    private Map<String, String> sensorIDToADUM = new HashMap<String, String>();
    private Map<String, String> ADUMToSensorID = new HashMap<String, String>();
    private final String ip;

    public ForwardingUnit(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 10);
        HttpContext spawnRequest = server.createContext("/adu/withModel");
        spawnRequest.setHandler(this::handleSpawnRequest);

        HttpContext subscriberRequest = server.createContext("/adu/subscriber");
        subscriberRequest.setHandler(this::handleGeneral);

        HttpContext aduSpawnSU = server.createContext("/adu/spawn");
        aduSpawnSU.setHandler(this::handleGeneral);

        HttpContext simpleADURequest = server.createContext("/adu");
        simpleADURequest.setHandler(this::handleGeneral);

        HttpContext anomalyReceiver = server.createContext("/anomaly");
        anomalyReceiver.setHandler(this::handleAnomalyForwarding);

        HttpContext sensorPointADU = server.createContext("/sensor/point");
        sensorPointADU.setHandler(this::handlePointForwarding);

        HttpContext test = server.createContext("/test");
        test.setHandler(this::handleGeneral);

        this.ip = getIPAddress(false);
        server.start();
        System.out.println("Edge Server: Started Forwarding Unit on port" + port);
    }

    private void handleAnomalyForwarding(HttpExchange exchange) throws IOException {
        String exchangedString = exchangeToString(exchange);
        System.out.println("Received Anomaly Message: " + exchangedString);
        System.out.println(receiverSensorMapping.keySet());
        System.out.println("TestingAnomalyGet: " + exchange.getRemoteAddress().getAddress().toString() + ":30001");
        String source = (exchange.getRemoteAddress().getAddress().toString() + ":30001").replace("/", "");
        String target = receiverSensorMapping.get(source).split(":")[0]+":20000";
        //System.out.println("Current target: " + target);
        sendAnomalyToAnRe(exchangedString, target);

    }

    private void sendAnomalyToAnRe(String exchangedString, String target) {
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            System.out.println("Sending anomaly to " + target);
            final HttpPost httppost = new HttpPost("http://" + target + "/sensor/point");



            httppost.setEntity(new StringEntity(exchangedString));

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
                    //System.out.println(body);
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

    private void handlePointForwarding(HttpExchange exchange) throws IOException {
        System.out.println("Forwarding Point");
        String exchangedMessage = exchangeToString(exchange);
        System.out.println(exchangedMessage);
        String sourceIP = exchange.getRemoteAddress().toString();
        String substring = sourceIP.replace("/", "").substring(0, sourceIP.replace("/", "").indexOf(":"));

        String target = getTargetFromID(exchangedMessage, substring);
        //System.out.println("Target: " + target);
        String anonymisedMessage = anonymize(exchangedMessage);
//        System.out.println("Anonymized Point Message");

        String answer = forwardSensorPoint(target, new JSONObject(anonymisedMessage));
//        System.out.println("Forwarded Point Message");
        sendAnswer(exchange, answer);
        System.out.println("Sent answer from Point Message");
    }

    private String getTargetFromID(String exchangedMessage, String substring) {
        JSONObject json = new JSONObject(exchangedMessage);
        String get = ((String) json.get("sensorAddress")).replace("http://", "");
//        System.out.println("Trying to get with: " + get);
        String sourceIP = this.sensorIDToADUM.get(get);
//        System.out.println("Keys: "+ this.sensorIDToADUM.keySet());
//        System.out.println("SourceIP in getSourceIPFromID: " + sourceIP);
        //System.out.println("SourceIP" + sourceIP);
        return sourceIP;
    }

    private String forwardSensorPoint(String target, JSONObject json) {
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpPost httppost = new HttpPost("http://" + target + "/sensor/point");

            System.out.println("Forwarding Point Message: " + json.toString() + " to " + target);
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
                    //System.out.println(body);
                    return body;
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
        return "";
    }

    private String anonymize(String exchangedMessage) {
        return exchangedMessage.replaceAll("10.0.0.65" + ":" + "[0-9]{4,5}", this.ip + ":" + 9999);
    }

    private void handleGeneral(HttpExchange exchange) throws IOException {
        System.out.println("Unhandled request in " + exchange.getHttpContext().getPath());
        try {
            System.out.println(exchangeToString(exchange));
        } catch (IOException e) {
            e.printStackTrace();
        }

        OutputStream outputStream = exchange.getResponseBody();
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<html>");
        // encode HTML content
        String htmlResponse = StringEscapeUtils.escapeHtml(htmlBuilder.toString());

        // this line is a must
        try {
            exchange.sendResponseHeaders(200, htmlResponse.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();

        outputStream.close();
    }


    public void setTunnel(String from, String to) {

        try {
            senderReceiverMapping.put(from, to);
            receiverSensorMapping.put(to, from);
            System.out.println("Edge Server: Added Tunnel from " + from + " to " + to);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleSpawnRequest(HttpExchange exchange) throws IOException {
        System.out.println("Received Spawn request from " + exchange.getRemoteAddress());
        String exchangedString = exchangeToString(exchange);
        String sourceIP = exchange.getRemoteAddress().toString();
        String substring = sourceIP.replace("/", "").substring(0, sourceIP.replace("/", "").indexOf(":"));

        JSONObject anonymisedBody = anonymizeExchangedString(exchangedString, substring);

        String source = getSource(exchangedString, substring);
        System.out.println("Source: " + source);
        String target = senderReceiverMapping.get(source);

        System.out.println("Forwarding Spawn request to " + target);
        String answer = forwardSpawnRequest(target, anonymisedBody);
        System.out.println("Sending answer back");
        sendAnswer(exchange, answer);
        System.out.println("Terminated Spawn Request");

    }

    private void sendAnswer(HttpExchange exchange, String answer) throws IOException {

        OutputStream outputStream = exchange.getResponseBody();

        // encode HTML content


        // this line is a must
        try {
            exchange.sendResponseHeaders(200, answer.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Sending answer: " + answer + " back.");
        outputStream.write(answer.getBytes());
        outputStream.flush();

        outputStream.close();
    }


    private String exchangeToString(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);

        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);
        }

        br.close();
        isr.close();
        return buf.toString();
    }

    private JSONObject anonymizeExchangedString(String exchangedString, String substring) {

        System.out.println("Edge Server: Anonymizing request from ip " + substring);
        JSONObject json = new JSONObject(exchangedString);
        JSONObject key = new JSONObject((String) json.get("key"));

        int id = Integer.parseInt(String.valueOf(key.get("ID")));
        String port = String.valueOf(29999 + id);
        String sourceIP_Port = substring + ":" + port;

        String targetIP = this.senderReceiverMapping.get(sourceIP_Port);
        sensorIDToADUM.put(((String) key.get("relatedSensorAddress")).replace("http://", ""), targetIP);
        //System.out.println("Set in SensorIDtoADUM the pair: (" + (String) key.get("relatedSensorAddress") + " : " + targetIP);
        ADUMToSensorID.put(targetIP, (String) key.get("relatedSensorAddress"));

        String jsonAsString = json.toString();
        jsonAsString = jsonAsString.replaceAll("10.0.0.65" + ":" + "[0-9]{4,5}", this.ip + ":" + 9999);
        JSONObject anonymizedJSON = new JSONObject(jsonAsString);
        //System.out.println(anonymizedJSON.toString(4));


        return anonymizedJSON;
    }

    private String getIPAddress(boolean useIPv4) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getSource(String exchangedString, String sourceIP) {

        JSONObject json = new JSONObject(exchangedString);
        JSONObject key = new JSONObject((String) json.get("key"));
        int id = Integer.parseInt(String.valueOf(key.get("ID")));
        String port = String.valueOf(29999 + id);
        return sourceIP + ":" + port;
    }

    private String forwardSpawnRequest(String target, JSONObject anonymizedJSON){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            // TODO replace fixed port
            System.out.println("Forwarding spawn request to target (IP:port) : " + target);
            final HttpPost httppost = new HttpPost("http://" + target + "/adu/withModel");
            //System.out.println(anonymizedJSON.toString(6));

            httppost.setEntity(new StringEntity(anonymizedJSON.toString()));

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
                    //System.out.println(body);
                    return body;
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
        return "Error";
    }

}
