

import org.jetbrains.annotations.NotNull;

public class EdgeNodeEntry implements Comparable<EdgeNodeEntry>{

    String communityID;
    String ESIP;
    String UUID;
    long freeMem;


    public EdgeNodeEntry(String uuid, long freeMem, String communityID, String ESIP) {
        this.UUID = uuid;
        this.freeMem = freeMem;
        this.ESIP = ESIP;
        this.communityID = communityID;





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

}
