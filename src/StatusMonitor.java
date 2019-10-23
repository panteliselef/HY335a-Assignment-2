
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
        System.out.println("Status Thread: "+ threadName);
    }

    private void setInterval(int millisecs){
        try {
            Thread.sleep(millisecs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendStatusRequest(String msg) throws IOException {
        GroupMember gm = mServer.getNextServer();
        if(gm==null)return;
        if(gm.getPort()==mServer.getPort() && gm.getIpAddress().equals(mServer.getIp()))return;

        //connect to server's right sibling
        Socket serverSocket = null;
        try{
            serverSocket  = new Socket(gm.getIpAddress(),gm.getPort()); // connect to another member
        }
        catch (ConnectException e){
            System.out.println("[send Status]Connect Exception");
            moveOn(msg,mServer.getNextOf(gm));
            mServer.getGroupMembers().remove(gm);
            System.out.println("--After moveOn()");
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

    public void moveOn(String msg,GroupMember gmServer) throws IOException{
        System.out.println("--Inside moveOn()");
        if(gmServer==null)return;
        if(gmServer.getPort()==mServer.getPort() && gmServer.getIpAddress().equals(mServer.getIp()))return;

        //connect to server's right sibling
        Socket serverSocket = null;
        try{
            serverSocket  = new Socket(gmServer.getIpAddress(),gmServer.getPort()); // connect to another member
        }
        catch (ConnectException e){
            System.out.println("[move On] Connect Exception");
            moveOn(msg,mServer.getNextOf(gmServer));
            mServer.getGroupMembers().remove(gmServer);
        }
        catch (SocketException e){
            System.out.println("[move On] Socket Exception");
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

    public static void moveOnToNext(String msg,WebServer mServer,GroupMember gmServer) throws IOException{
        System.out.println("--Inside moveOnNext()");
        if (gmServer== null)return;

        Socket serverSocket = null;
        try{
            serverSocket  = new Socket(gmServer.getIpAddress(),gmServer.getPort()); // connect to another member
        }
        catch (ConnectException e){
            System.out.println("[Static moveOn] Connect Exception");
            moveOnToNext(msg,mServer,mServer.getNextOf(gmServer));
            mServer.getGroupMembers().remove(gmServer);
        }
        catch (SocketException e){
            System.out.println("[Static moveOn] Socket Exception");
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


    public static void sendReq(String msg, WebServer mServer) throws IOException{
        GroupMember gm = mServer.getNextServer();
        if(gm==null)return;
        if(gm.getPort()==mServer.getPort() && gm.getIpAddress().equals(mServer.getIp()))return;

        //connect to server's right sibling
        Socket serverSocket = null;
        try{
            serverSocket  = new Socket(gm.getIpAddress(),gm.getPort()); // connect to another member
        }
        catch (ConnectException e){
            System.out.println("[Static Req] Connect Exception");
            moveOnToNext(msg,mServer,mServer.getNextOf(gm));
            mServer.getGroupMembers().remove(gm);
            System.out.println("--After moveOnNext()");
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
                sendStatusRequest("");
            }catch (IOException e){
                e.printStackTrace();
            }
            setInterval(5000);
        }

    }
}