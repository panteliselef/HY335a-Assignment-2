import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class WebServer {
    private String name;
    private int port;
    private String ip;
    private int connectPort;
    private String connectIp;
    private ArrayList<GroupMember> groupMembers = new ArrayList<>();
    private HashMap<String,VirtualFile> memory = new HashMap<>();
    private WebServer(String name, int port, String ip){
        this.name = name;
        this.ip = ip;
        this.port = port;
    }


    public HashMap<String, VirtualFile> getMemory() {
        return memory;
    }

    public GroupMember getThisServer(){
        return new GroupMember(name,port,ip);
    }

    @Override
    public String toString() {
        return "[WebServer]"+name+" with "+ip+" on "+port;
    }

    public GroupMember getNextOf(GroupMember gm){
        int index = groupMembers.indexOf(gm);
        int size = groupMembers.size();
        if(size == 0)return null;
        if( (size - index) == 1){
            return groupMembers.get(0);
        }else return groupMembers.get(index +1);
    }

    public GroupMember getNextServer(){
        int size = groupMembers.size();
        int index = getIndexAsMember();
        if(size == 0)return null;
        if( (size - index) == 1){
            // this server is the last node
            // so the next member should be the first node
            return groupMembers.get(0);
        }else {
            //is not last node
            return groupMembers.get(index+1);
        }
    }

    private void configConnect(String connectIp, int connectPort){
        this.connectIp= connectIp;
        this.connectPort = connectPort;
    }
    public int getIndexAsMember(){
        int index = 0;
        for (GroupMember gm:groupMembers) {
            if(name.equals(gm.getName())){
                index = groupMembers.indexOf(gm);
            }
        }
        return index;
    }

    public int getConnectPort() {
        return connectPort;
    }

    public String getConnectIp() {
        return connectIp;
    }

    public void setGroupMembers(ArrayList<GroupMember> groupMembers) {
        this.groupMembers = groupMembers;
    }

    public void storeFile(VirtualFile vf){
        if(memory.containsKey(vf.getName())){
            //need to update the content
            memory.replace(vf.getName(),vf);
            System.out.println(vf.getName()+" has been updated in "+name);
        }else{
            memory.put(vf.getName(),vf);
            System.out.println(vf.getName()+" has been saved in "+name);
        }

    }

    public void showGroupMembers(){
        System.out.println("Printing Members of "+name);
        for (GroupMember gm:
             groupMembers) {
            System.out.println(gm);
        }
    }
    public void showMemomry(){
        System.out.println("Printing Memory of "+name);
        memory.forEach((key,value) -> System.out.println("Name: "+key+" | Content: "+value.getContent()+" | Created: "+value.getTimestamp().toEpochMilli()));
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
    }

    public ArrayList<GroupMember> getGroupMembers() {
        return groupMembers;
    }

    private void joinGroup() {
        JoinMonitor jm = new JoinMonitor(name,this.getConnectIp(),this.getConnectPort(),this);
        jm.start();
    }

    private void listen(int port)  {
        ListenMonitor lm = new ListenMonitor(name,port, this);
        lm.start();
    }

    public static void main(String argv[]) {
        // java WebServer localhost 3942 "Web Server 1"
        // java WebServer localhost 3942 localhost 4042 "Web Server 2"
        String ipAddress = "localhost";
        String serverName = "";
        int port = 3942;
        String connectIp = "";
        int connectPort = 0;

        if (argv.length == 3) {
            ipAddress = argv[0];
            port = Integer.parseInt(argv[1]);
            serverName = argv[2];
        } else if (argv.length == 5) {
            ipAddress = argv[0];
            port = Integer.parseInt(argv[1]);
            connectIp = argv[2];
            connectPort = Integer.parseInt(argv[3]);
            serverName = argv[4];
        } else {
            System.exit(0);
        }

        WebServer wb = new WebServer(serverName,port,ipAddress);
        StatusMonitor sm = new StatusMonitor(serverName,wb);
        if(argv.length == 3){
            wb.getGroupMembers().add(new GroupMember(serverName,port,ipAddress));
            sm.start();
        }

        wb.listen(wb.getPort());
        if(argv.length == 5){
            wb.configConnect(connectIp,connectPort);
            wb.joinGroup();
            sm.start();
        }

    }
}
