/*
 * Pantelis Eleftheriadis
 * csd3942
 */
import java.util.ArrayList;

public class GroupMember {

    private String name;
    private int port;
    private String ipAddress;

    GroupMember(String name, int port, String ipAddress){
        this.name = name;
        this.port = port;
        this.ipAddress = ipAddress;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean equals(GroupMember obj) {
        if (this.name.equals(obj.name) && this.port == obj.port && this.ipAddress.equals(obj.ipAddress)) return true;
        return false;
    }

    public static GroupMember parseGroupMember(String memberAsStr){
        String[] info = memberAsStr.split("[,]");
        return new GroupMember(info[0], Integer.parseInt(info[1]), info[2]);
    }

    public static ArrayList<GroupMember> parseGroupMembers(String membersAsStr){
        String[] items = membersAsStr.split("[$]");
        ArrayList<GroupMember> gms = new ArrayList<>();
        for (String item : items) {
            gms.add(parseGroupMember(item));
        }
        return gms;
    }

    @Override
    public String toString() {
        return "[GroupMember]"+name+" with "+ipAddress+" on "+port;
    }
}
