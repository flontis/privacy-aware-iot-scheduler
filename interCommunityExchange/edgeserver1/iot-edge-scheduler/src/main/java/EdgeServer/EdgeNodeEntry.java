package EdgeServer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.PriorityQueue;

public class EdgeNodeEntry implements Comparable<EdgeNodeEntry>{

    String UUID;
    long maxMem;
    long freeMem;
    long maxFreq;
    PriorityQueue<ADUContainer> containers = new PriorityQueue<>();

    public EdgeNodeEntry(JSONObject dataFromCapRep, String decryptedUUID) {
        this.UUID = decryptedUUID;
        JSONObject host = new JSONObject(dataFromCapRep.get("Host").toString());
//        System.out.println(dataFromCapRep.toString(4));
        this.freeMem = ((Number) host.get("freeMem")).longValue();
        this.maxMem = ((Number) host.get("maxMem")).longValue();
        this.maxFreq = ((Number) host.get("maxFrequency")).longValue();;

        JSONObject cpu = new JSONObject(dataFromCapRep.get("CPU").toString());
//        System.out.println("Containers: " + cpu.keySet());
        JSONObject mem = new JSONObject(dataFromCapRep.get("Memory").toString());

        for (String container: cpu.keySet()){
            if(container.contains("adu")) {
                String containerName = container.replace("/", "");
                JSONObject containerjson = new JSONObject(mem.get(container).toString());
                long memUsage = (long) (int) containerjson.get("memUsage");

                double cpuUsage = Double.parseDouble(cpu.get(container).toString().replace("%", ""));
//                System.out.println("Add new container " + container + " with ID " + decryptedUUID + " with memUsage of " + memUsage + " and cpuUsage of " + cpuUsage);
                ADUContainer aduContainer = new ADUContainer(decryptedUUID, containerName, memUsage, cpuUsage);
                containers.add(aduContainer);
            }
        }
//        System.out.println("PQ content is: ");
        printPriorityQueue();

    }

    @Override
    public int compareTo(@NotNull EdgeNodeEntry edgeNodeEntry) {

        if (this.freeMem > edgeNodeEntry.freeMem)
            return -1;
        else if (this.freeMem < edgeNodeEntry.freeMem)
            return 1;
        else
        return 0;
    }

    public void printPriorityQueue(){
        PriorityQueue<ADUContainer> tmp = new PriorityQueue<>(this.containers);
        while(!tmp.isEmpty()){
            ADUContainer aduc = tmp.poll();
            System.out.println(aduc.toString());
        }
    }
}
