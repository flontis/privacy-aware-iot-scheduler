package ResourceAdministrator;

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
import java.util.Random;

public class Sensor {
    public static void main(String[] args) throws InterruptedException {
        while(true) {
            Thread.sleep(50);
            Random r = new Random();
            double x = 0 + (100000) * r.nextDouble();
            double y = 0 + (100000) * r.nextDouble();
            sendPoint(x, y);
        }
    }

    private static void sendPoint(double x, double y){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpPost httppost = new HttpPost("http://localhost:" + 20000 + "/sensor/point");

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
}
