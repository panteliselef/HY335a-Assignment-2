/*
 * Pantelis Eleftheriadis
 * csd3942
 */
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientConnectionInfo {

    private Socket openedSocket;
    private DataOutputStream stream2Client;

    ClientConnectionInfo(Socket os, DataOutputStream s2c){
        this.openedSocket = os;
        this.stream2Client = s2c;
    }


    public Socket getOpenedSocket() {
        return openedSocket;
    }

    public DataOutputStream getOutStream() {
        return stream2Client;
    }
}
