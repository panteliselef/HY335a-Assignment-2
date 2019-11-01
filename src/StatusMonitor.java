
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;


public class StatusMonitor extends Thread {

    private String threadName;
    private WebServer mServer;

    StatusMonitor(String threadName,WebServer server){
        this.threadName = "[Status Monitor] "+threadName;
        this.mServer = server;
    }

    private void setInterval(int millisecs){
        try {
            Thread.sleep(millisecs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendStatusRequest(String msg,GroupMember gm) throws IOException {
        if(gm==null)return;
        if(gm.getPort()==mServer.getPort() && gm.getIpAddress().equals(mServer.getIp()))return;

        //connect to server's right sibling
        Socket serverSocket = null;
        try{
            serverSocket  = new Socket(gm.getIpAddress(),gm.getPort()); // connect to another member
        }
        catch (ConnectException e){
            System.out.println(ConsoleColors.RED+ "[send Status] Cannot connect to "+ gm.getName()+ ConsoleColors.RESET);
            GroupMember nextOfGm = mServer.getNextOf(gm);
            mServer.getGroupMembers().remove(gm);
            sendStatusRequest(msg,nextOfGm);
        }
        catch (SocketException e){
            System.out.println("[send Status]Socket Exception");
        }
        if(serverSocket == null)return;
        DataOutputStream outToMember = new DataOutputStream(serverSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader( serverSocket.getInputStream()));
        // Send ELEF to other member
        outToMember.writeBytes("ELEF\n");
        // Send JOIN to other member to register as new member
        outToMember.writeBytes("STATUS received from "+mServer.getName() +"\n");
        outToMember.writeBytes(msg+mServer.getName()+","+mServer.getPort()+","+mServer.getIp()+"$\n");
        serverSocket.close();
    }

    public static void sendReq(String msg, WebServer mServer,GroupMember gm) throws IOException{
        if(gm==null)return;
        if(gm.getPort()==mServer.getPort() && gm.getIpAddress().equals(mServer.getIp()))return;

        //connect to server's right sibling
        Socket serverSocket = null;
        try{
            serverSocket  = new Socket(gm.getIpAddress(),gm.getPort()); // connect to another member
        }
        catch (ConnectException e){
            System.out.println(ConsoleColors.RED+ "[Static Req] Cannot connect to "+ gm.getName()+ ConsoleColors.RESET);
            GroupMember nextOfGm = mServer.getNextOf(gm);
            mServer.getGroupMembers().remove(gm);
            sendReq(msg,mServer,nextOfGm);
        }
        catch (SocketException e){
            System.out.println("[Static Req] Socket Exception");
        }
        if(serverSocket == null)return;
        DataOutputStream outToMember = new DataOutputStream(serverSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader( serverSocket.getInputStream()));
        // Send ELEF to other member
        outToMember.writeBytes("ELEF\n");

        outToMember.writeBytes("STATUS received from "+mServer.getName() +"\n");
        outToMember.writeBytes(msg+mServer.getName()+","+mServer.getPort()+","+mServer.getIp()+"$\n");
        serverSocket.close();

    }

    @Override
    public void run() {
        System.out.println("Running "+ threadName);
        while(true){
            try {
                GroupMember gm = mServer.getNextServer();
                sendStatusRequest("",gm);
            }catch (IOException e){
                e.printStackTrace();
            }
            setInterval(5000);
        }

    }
}