import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class JoinMonitor extends Thread {

    private String threadName;
    private String connectIp;
    private int connectPort;
    private WebServer mServer; // need this to change values by reference
    JoinMonitor(String threadName,String connectIp,int connectPort, WebServer mServer){
        this.threadName = "[Join Monitor] "+threadName;
        this.connectIp = connectIp;
        this.connectPort = connectPort;
        this.mServer = mServer;
        System.out.println("Joining "+ threadName);
    }

    // this method was added to remove duplicate code in run()
    private ArrayList<GroupMember> fetchMembers(String lineFromServer){
        String[] items = lineFromServer.split("[$]");
        ArrayList<GroupMember> gms = new ArrayList<>();
        for (String item: items) {
            String[] info = item.split("[,]");
            GroupMember gm = new GroupMember(info[0],Integer.parseInt(info[1]),info[2]);
            gms.add(gm);
        }
        return gms;
    }

    @Override
    public void run() {
        System.out.println("Joining "+ threadName);
        String requestMessageLine;
        try {
            Socket serverSocket  = new Socket(connectIp,connectPort); // connect to another member
            DataOutputStream outToMember = new DataOutputStream(serverSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader( serverSocket.getInputStream()));

            // Send ELEF to other member
            outToMember.writeBytes("ELEF\n");
            // Send JOIN to other member to register as new member
            outToMember.writeBytes("JOIN\n");

            // response from the other member
            requestMessageLine = inFromServer.readLine();
            if(requestMessageLine != null){
                if(Integer.parseInt(requestMessageLine) == 1){
                    //server doens't have right sibling
                    //fetch the member list from the other member
                    //and add this server's data to your list
                    //and also to the other member LAST
                    requestMessageLine = inFromServer.readLine();
                    ArrayList<GroupMember> gms = fetchMembers(requestMessageLine);
                    outToMember.writeBytes(mServer.getName()+","+mServer.getPort()+","+mServer.getIp()+",\n");
                    gms.add(new GroupMember(mServer.getName(),mServer.getPort(),mServer.getIp()));
                    mServer.setGroupMembers(gms);
                    System.out.println(ConsoleColors.BLUE + "JOINED"+ ConsoleColors.RESET);
                    mServer.showGroupMembers();
                }else{
                    //server HAS right sibling
                    //fetch the member list from the other member
                    //and add this server's data to your list
                    //and also to the other member BETWEEN two members
                    requestMessageLine = inFromServer.readLine(); // list of servers
                    ArrayList<GroupMember> gms = fetchMembers(requestMessageLine);
                    int indexToAdd = 0;
                    for (GroupMember gm:gms) {
                        if(gm.getIpAddress().equals(connectIp) && gm.getPort() == connectPort ){
                            indexToAdd = gms.indexOf(gm);
                        }
                    }
                    GroupMember left = gms.get(indexToAdd);
                    GroupMember right = gms.get(indexToAdd+1);
                    gms.add(indexToAdd+1,new GroupMember(mServer.getName(),mServer.getPort(),mServer.getIp()));
                    outToMember.writeBytes(mServer.getName()+","+mServer.getPort()+","+mServer.getIp()+",\n");
                    mServer.setGroupMembers(gms);
                    System.out.println(ConsoleColors.BLUE + "JOINED"+ ConsoleColors.RESET);
                    mServer.showGroupMembers();
                }
            }
            else System.out.println(ConsoleColors.RED+"Nothing to show "+ConsoleColors.RESET);
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
