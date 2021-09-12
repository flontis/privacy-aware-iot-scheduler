package EdgeServer;

import org.jetbrains.annotations.NotNull;

public class ADUContainer implements Comparable<ADUContainer>{

    String ENuuid;
    String name;
    long memUsage;
    double CPUUsage;

    public ADUContainer(String uuid, String name, long memUsage, double CPUUsage){
        this.ENuuid = uuid;
        this.name = name;
        this.memUsage = memUsage;
        this.CPUUsage = CPUUsage;
    }


    @Override
    public int compareTo(@NotNull ADUContainer aduContainer) {
        if (this.memUsage > aduContainer.memUsage)
            return -1;
        else if (this.memUsage < aduContainer.memUsage)
            return 1;
        else
            return 0;
    }

    @Override
    public String toString(){
        return "ADUContainer " + this.name + " of ID " + this.ENuuid + "with memUsage of " + this.memUsage;
    }
}
