/*
 * Pantelis Eleftheriadis
 * csd3942
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;


public class MemoryMonitor extends Thread {
    public enum ReqType {GET, PUT};
    private String threadName;
    private WebServer mServer;
    private VirtualFile vf;
    private ReqType type;
    private String customMsg;

    MemoryMonitor(String threadName, ReqType type, WebServer mServer, VirtualFile vf) {
        if (type == ReqType.GET) this.threadName = "[Memomry Monitor GET] " + threadName;
        else this.threadName = "[Memomry Monitor PUT] " + threadName;
        this.mServer = mServer;
        this.vf = vf;
        this.type = type;
    }

    MemoryMonitor(String threadName, ReqType type, WebServer mServer, VirtualFile vf, String customMsg) {
        if (type == ReqType.GET) this.threadName = "[Memomry Monitor GET] " + threadName;
        else this.threadName = "[Memomry Monitor PUT] " + threadName;
        this.mServer = mServer;
        this.vf = vf;
        this.type = type;
        this.customMsg = customMsg;
    }

    private void writeFilePutMessage(DataOutputStream dops) throws IOException {
        dops.writeBytes("ELEF\n");

        dops.writeBytes("FILE PUT received from " + mServer.getName() + "\n");
        dops.writeBytes("FILENAME:" + vf.getName() + '\n');
        dops.writeBytes("CONTENT:" + vf.getContent() + '\n');
        dops.writeBytes("TIMESTAMP:" + vf.getTimestamp().toEpochMilli() + '\n');
    }

    private void writeFileGetMessage(DataOutputStream dops) throws IOException {
        dops.writeBytes("ELEF\n");

        dops.writeBytes("FILE GET received from " + mServer.getName() + "\n");
        dops.writeBytes("REQNAME:" + vf.getName() + '\n');
    }

    private void writeFileGetCustomMessage(DataOutputStream dops, String msg) throws IOException {
        //msg should end with '\n' or to be empty ""
        dops.writeBytes("ELEF\n");

        dops.writeBytes("FILE GET received from " + mServer.getName() + "\n");
        dops.writeBytes("REQNAME:" + vf.getName() + '\n');
        dops.writeBytes(msg);
    }
    public void sendFileRequest(String senderLine, GroupMember gm) throws IOException {
        //connect to server's right sibling
        Socket serverSocket = null;
        try {
            serverSocket = new Socket(gm.getIpAddress(), gm.getPort()); // connect to another member
        } catch (ConnectException e) {
            System.out.println("[send File]Connect Exception");
            System.out.println("[send File]Sending to"+mServer.getNextOf(gm));
            sendFileRequest(senderLine, mServer.getNextOf(gm));
            System.out.println("--After sendFileRequest()");
        } catch (SocketException e) {
            System.out.println("[send File]Socket Exception");
        }
        if (serverSocket == null) return;
        DataOutputStream outToMember = new DataOutputStream(serverSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        // Send ELEF to other member
        if (type == ReqType.PUT) {
            writeFilePutMessage(outToMember);
            outToMember.writeBytes(senderLine);
        } else if (type == ReqType.GET) {
            if (customMsg == null) {
                writeFileGetMessage(outToMember);
                outToMember.writeBytes(senderLine);
            } else writeFileGetCustomMessage(outToMember, customMsg);
        }

        serverSocket.close();
    }


    @Override
    public void run() {
        try {
            GroupMember gm = mServer.getNextServer();
            sendFileRequest("SENDER:" + mServer.getName(), gm);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
