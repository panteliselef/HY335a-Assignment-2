import java.io.DataOutputStream;
import java.net.Socket;

public class HttpPutResponse {

    private Socket openedSocket;
    private DataOutputStream stream2Client;

    HttpPutResponse(Socket os, DataOutputStream s2c){
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
