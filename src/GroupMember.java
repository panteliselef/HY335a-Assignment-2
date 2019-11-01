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

    @Override
    public String toString() {
        return "[GroupMember]"+name+" with "+ipAddress+" on "+port;
    }
}
