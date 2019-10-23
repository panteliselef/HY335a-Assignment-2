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

    @Override
    public String toString() {
        return "[GroupMember]"+name+" with "+ipAddress+" on "+port;
    }
}
