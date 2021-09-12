package Testclasses;

import EdgeServer.ESWebSocketServer;
import ResourceAdministrator.ResourceMonitor;

import java.net.URISyntaxException;

public class Starter {
    public static void main(String[] args) {
        ESWebSocketServer es = null;
        try {
            es = new ESWebSocketServer();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        es.start();


        ResourceMonitor rm = new ResourceMonitor();

        System.out.println("CREATED RM1");
        ResourceMonitor rm2 = new ResourceMonitor();
        System.out.println("CREATED RM2");

    }

}
