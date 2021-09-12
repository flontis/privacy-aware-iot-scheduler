package ResourceAdministrator;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HTTPRequester {

    private final int sensor = 20000;
    private final int adum1 = 30000;
    private final int adum2 = 30001;
    private final int anre = 40000;

    public HTTPRequester() {
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("HI");
        Thread.sleep(1000);
        HTTPRequester req = new HTTPRequester();
//        req.getRequest(20000);
        req.spawnADU();
        Thread.sleep(2000);
        req.sendPoint(1.0, 2000006.5);
        req.sendPoint(80.0, 90.5);
        req.sendPoint(500.0, 30.5);
        Thread.sleep(2000);
        req.getADU();
        Thread.sleep(2000);
        req.rescheduleADU("adum1", "adum2", 30000, 30001, "anre", 40000);
        System.out.println("------------------Rescheduling done------------------");
//        req.sendPoint(88.0, 77);
        System.out.println("New get Request");
        req.getNewADU();
//        req.deleteADU();

    }

    private void rescheduleADU(String startADU, String targetADU, int startADUport, int targetADUport, String anre, int anrePort){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpPost httppost = new HttpPost("http://192.168.1.218:" + startADUport + "/adu/reschedule");

            JSONObject json = new JSONObject();
            json.put("relatedSensorID", 1);
            json.put("relatedSensorAddress", "http://192.168.1.218:20000");
            json.put("algorithm", "birch");
            json.put("ID", 1);
            json.put("adumAddress", "http://" + "192.168.1.29" + ":" + 30001);
            json.put("relatedSensorMeta", "");
            json.put("anomalyReceiver", Arrays.asList("http://192.168.1.218:" + anrePort));
            json.put("reschedulingTimeout", 0);

            httppost.setEntity(new StringEntity(json.toString()));
            System.out.println(json.toString(4));
            System.out.println("Posting Reschedule Request");
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

    private void deleteADU(){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpDeleteWithEntity httppost = new HttpDeleteWithEntity();
            httppost.setURI(URI.create("http://192.168.1.218:" + adum1 + "/allADU"));

            JSONObject json = new JSONObject();
            json.put("relatedSensorID", 1);
            json.put("relatedSensorAddress", "http://su:20000");
            json.put("algorithm", "birch");
            json.put("ID", 1);
            json.put("adumAddress", "http://adum1:30000");
            json.put("anomalyReceiver", Arrays.asList("http://anre:40000"));
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
    private void getADU(){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpGetWithEntity httpget = new HttpGetWithEntity();
            httpget.setURI(URI.create("http://192.168.1.218:" + adum1 + "/adu"));
            System.out.println("Executing request " + httpget.getMethod() + " " + httpget.getURI());

            JSONObject json = new JSONObject();
            json.put("relatedSensorID", 1);
            json.put("relatedSensorAddress", "http://192.168.1.218:20000");
            json.put("algorithm", "birch");
            json.put("ID", 1);
            json.put("adumAddress", "http://192.168.1.218:30000");
            json.put("anomalyReceiver", Arrays.asList("http://192.168.1.218:40000"));

            httpget.setEntity(new StringEntity(json.toString()));

            CloseableHttpResponse response = httpclient.execute(httpget);
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

    private void spawnADU(){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpPost httppost = new HttpPost("http://localhost:" + sensor + "/adu/spawn");

            JSONObject json = new JSONObject();
            json.put("relatedSensorID", 1);
            json.put("relatedSensorAddress", "http://192.168.1.218:20000");
            json.put("algorithm", "birch");
            json.put("ID", 1);
            json.put("adumAddress", "http://192.168.1.218:30000");
            json.put("anomalyReceiver", Arrays.asList("http://192.168.1.218:40000"));

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

    private void getRequest(int port) {
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
            final HttpGet httpget = new HttpGet("http://localhost:" + port+ "/");



            System.out.println("Executing request " + httpget.getMethod() + " " + httpget.getURI());
            try (final CloseableHttpResponse response = httpclient.execute(httpget)) {
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public class HttpGetWithEntity extends HttpEntityEnclosingRequestBase {
        public final static String METHOD_NAME = "GET";

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }
    }

    public class HttpDeleteWithEntity extends HttpEntityEnclosingRequestBase {
        public final static String METHOD_NAME = "DELETE";

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }
    }

    private void sendPoint(double x, double y){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpPost httppost = new HttpPost("http://localhost:" + sensor + "/sensor/point");

            JSONObject json = new JSONObject();
            JSONObject inner = new JSONObject();
            inner.put("x", x);
            inner.put("y", y);
            json.put("value", inner);

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

    private void getNewADU(){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpGetWithEntity httpget = new HttpGetWithEntity();
            httpget.setURI(URI.create("http://192.168.1.29:" + adum2 + "/adu"));
            System.out.println("Executing request " + httpget.getMethod() + " " + httpget.getURI());

            JSONObject json = new JSONObject();
            json.put("relatedSensorID", 1);
            json.put("relatedSensorAddress", "http://192.168.1.218:20000");
            json.put("algorithm", "birch");
            json.put("ID", 1);
            json.put("adumAddress", "http://192.168.1.29:30001");
            json.put("anomalyReceiver", Arrays.asList("http://192.168.1.218:40000"));

            httpget.setEntity(new StringEntity(json.toString()));

            CloseableHttpResponse response = httpclient.execute(httpget);
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
