import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;

public class WebClient {

    private int connectPort;
    private String connectIp;
    private HashMap<String, String> httpReqMap = new HashMap<>();
    public WebClient(String connectIp,int connectPort){
        this.connectIp = connectIp;
        this.connectPort = connectPort;
    }

    public HashMap<String, String> getHttpReqMap() {
        return httpReqMap;
    }

    public static void main(String argv[]) throws Exception {
        String sentence;
        String modifiedSentence;
        String bigSentence = "";

        String connectIp = "";
        int connectPort = 0;

        if (argv.length == 2) {
            connectIp = argv[0];
            connectPort = Integer.parseInt(argv[1]);
        } else {
            System.out.println("Invalid parameter");
            System.exit(0);
        }

        WebClient wc = new WebClient(connectIp,connectPort);

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = null;
        try {
            clientSocket = new Socket(connectIp, connectPort);
        }catch (ConnectException e){
            System.out.println("Unable to connect to "+connectIp+ " on "+connectPort);
        }
        if (clientSocket == null)return;

        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));



        sentence = inFromUser.readLine(); // PUT
        if (sentence.contains("GET")) {
            bigSentence = bigSentence.concat(sentence + '\n');

            outToServer.writeBytes(bigSentence);
        } else if (sentence.contains("PUT")) {
            bigSentence = bigSentence.concat(sentence +'\n');
            outToServer.writeBytes(bigSentence);
            for (int i = 0; i < 2; i++) {
                String[] pairs;
                sentence = inFromUser.readLine();
                pairs = sentence.split(":");
                wc.getHttpReqMap().put(pairs[0], pairs[1]);
            }

            int numOfBytes = 0;
            if (wc.getHttpReqMap().get("Content-length") != null) {
                numOfBytes = Integer.parseInt(wc.getHttpReqMap().get("Content-length").replaceAll(" ", ""));

            }

            System.out.println(numOfBytes);
            for (String key : wc.getHttpReqMap().keySet()) {
                System.out.print(key + ":" + wc.getHttpReqMap().get(key) + "\n");
                outToServer.writeBytes(key + ":" + wc.getHttpReqMap().get(key) + "\n");
            }

            char[] content = new char[numOfBytes];
            byte[] byteContent = new byte[numOfBytes];
            inFromUser.read(content); // html code

            for (int i = 0; i < numOfBytes; i++) {
                System.out.print(content[i]);
                byteContent[i] = (byte) content[i];
            }
            System.out.print("\n");
            try {
                outToServer.write(byteContent);

            } catch (IOException e) {
                System.out.println("Cannot write bytes from client to server");

            }
        }

        while ((modifiedSentence = inFromServer.readLine()) != null) {
            System.out.println("FROM SERVER: " + modifiedSentence);
        }

        clientSocket.close();

    }
}
