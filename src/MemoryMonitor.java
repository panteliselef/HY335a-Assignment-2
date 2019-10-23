import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class MemoryMonitor extends Thread {

    private String threadName;
    private WebServer mServer;
    private VirtualFile vf;

    MemoryMonitor(String threadName, WebServer mServer, VirtualFile vf) {
        this.threadName = "[Memomry Monitor] " + threadName;
        this.mServer = mServer;
        this.vf = vf;

        System.out.println("Memory Thread: " + threadName);
    }

    private void writeFileMessage(DataOutputStream dops) throws IOException {
        dops.writeBytes("ELEF\n");

        dops.writeBytes("FILE received from " + mServer.getName() + "\n");
        dops.writeBytes("FILENAME:" + vf.getName() + '\n');
        dops.writeBytes("CONTENT:" + vf.getContent() + '\n');
        dops.writeBytes("TIMESTAMP:" + vf.getTimestamp().toEpochMilli() + '\n');
    }

    //    private void sendFileRequest() throws IOException {
//        GroupMember gm = mServer.getNextServer();
//        if(gm==null)return;
//        if(gm.getPort()==mServer.getPort() && gm.getIpAddress().equals(mServer.getIp()))return;
//
//        //connect to server's right sibling
//        Socket serverSocket = null;
//        try{
//            serverSocket  = new Socket(gm.getIpAddress(),gm.getPort()); // connect to another member
//        }
//        catch (ConnectException e){
//            System.out.println("[send File]Connect Exception");
//            moveOn(mServer.getNextOf(gm));
//            mServer.getGroupMembers().remove(gm);
//            System.out.println("--After moveOn()");
//        }
//        catch (SocketException e){
//            System.out.println("[send File]Socket Exception");
//        }
//        if(serverSocket == null)return;
//        DataOutputStream outToMember = new DataOutputStream(serverSocket.getOutputStream());
//        BufferedReader inFromServer = new BufferedReader(new InputStreamReader( serverSocket.getInputStream()));
//        // Send ELEF to other member
//        writeFileMessage(outToMember);
//
//        serverSocket.close();
//    }
    public void sendFileRequest(String senderLine,GroupMember gm) throws IOException {
        if(gm == mServer.getThisServer()) return;
        if (gm == null) return;
        if (gm.getPort() == mServer.getPort() && gm.getIpAddress().equals(mServer.getIp())) return;

        //connect to server's right sibling
        Socket serverSocket = null;
        try {
            serverSocket = new Socket(gm.getIpAddress(), gm.getPort()); // connect to another member
        } catch (ConnectException e) {
            System.out.println("[send File]Connect Exception");
            sendFileRequest(senderLine,mServer.getNextOf(gm));
            System.out.println("--After sendFileRequest()");
        } catch (SocketException e) {
            System.out.println("[send File]Socket Exception");
        }
        if (serverSocket == null) return;
        DataOutputStream outToMember = new DataOutputStream(serverSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        // Send ELEF to other member
        writeFileMessage(outToMember);
        outToMember.writeBytes(senderLine);

        serverSocket.close();
    }



    @Override
    public void run() {
//        while (true) {
            try {
                GroupMember gm = mServer.getNextServer();
                sendFileRequest("SENDER:"+mServer.getName(),gm);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            try {
//                Thread.sleep(20000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

//        }
    }
}
