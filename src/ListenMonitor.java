/*
 * Pantelis Eleftheriadis
 * csd3942
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;


/**
 * This class implements the listening functionality of a WebServer.
 * It extends the class Thread, because we don't want to "listen" in the main Thread and use another on instead
 * It has a threadName, a port number, a reference to a Server, and it uses hashmaps to save/organize
 * pending client requests
 * @author csd3942
 */

public class ListenMonitor extends Thread {
    private String threadName;
    private int port;
    private WebServer mServer;
    private HashMap<Long, ClientConnectionInfo> pendingClientPutReq = new HashMap<>();
    private HashMap<String,ArrayList<ClientConnectionInfo>> pendingClientGetReq = new HashMap<>();

    ListenMonitor(String threadName, int port, WebServer mServer) {
        this.threadName = "[Listen Monitor] " + threadName;
        this.port = port;
        this.mServer = mServer;
    }

    /**
    * @post Client/Clients should get an answer from Server and the pending request should have been removed as it is no longer pending
    * @param filename name of the file we want to send to Client/Clients
    * */
    private void answerGETToClient(String filename) throws IOException{
        if(pendingClientGetReq.containsKey(filename)){
            ArrayList<ClientConnectionInfo> hprs = pendingClientGetReq.get(filename);

            for (ClientConnectionInfo res:hprs
            ) {
                Socket toBeClosed = res.getOpenedSocket();
                DataOutputStream out = res.getOutStream();
                if(mServer.getMemory().containsKey(filename)){

                    VirtualFile requestedFile = mServer.getMemory().get(filename);

                    out.writeBytes("HTTP/1.1 200 Document Follows\r\n");
                    if (filename.endsWith(".jpg"))
                        out.writeBytes("Content-Type: image/jpeg\r\n");
                    if (filename.endsWith(".gif"))
                        out.writeBytes("Content-Type: image/gif\r\n");
                    out.writeBytes("Content-Length: " + requestedFile.getContent().length() + "\r\n");
                    out.writeBytes("\r\n");


                    out.write(requestedFile.getContent().getBytes(), 0, requestedFile.getContent().length());
                }else {
                    out.writeBytes("HTTP/1.1 404 File Not Found\r\n");
                }
                toBeClosed.close();
            }
            pendingClientGetReq.remove(filename);
        }
    }


    private void handleClientPUT(Socket connectionSocket,StringTokenizer tokenizedLine) throws IOException{
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

        String fileName = "";
        String requestMessageLine;
        ClientConnectionInfo res = new ClientConnectionInfo(connectionSocket, outToClient);
        fileName = tokenizedLine.nextToken();
        System.out.println(ConsoleColors.BLUE+"HTTP PUT "+fileName+ConsoleColors.RESET);

        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        requestMessageLine = inFromClient.readLine(); // Content-Type
        requestMessageLine = inFromClient.readLine(); // Content-length
        StringTokenizer tok = new StringTokenizer(requestMessageLine);
        tok.nextToken();
        int numOfBytes = Integer.parseInt(tok.nextToken());
        char[] content = new char[numOfBytes];

        inFromClient.read(content); // read bytes
        VirtualFile vf = new VirtualFile(fileName, String.copyValueOf(content));

        mServer.getMemory().put(fileName, vf);
        mServer.showMemomry();


        if (mServer.getGroupMembers().size() == 1) {
            outToClient.writeBytes("HTTP/1.1 201 Created\r\n");

            if (fileName.endsWith(".jpg"))
                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
            if (fileName.endsWith(".gif"))
                outToClient.writeBytes("Content-Type: image/gif\r\n");

            outToClient.writeBytes("Content-Location: /" + fileName + "\r\n");
//            pendingClientPutReq.remove(vf.getTimestamp().toEpochMilli());
            connectionSocket.close();
        }else{
            pendingClientPutReq.put(vf.getTimestamp(), res);
            MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.PUT, mServer, vf);
            mm.start();
        }
    }

    private void handleClientGET(Socket connectionSocket,StringTokenizer tokenizedLine) throws IOException{

        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

        String fileName = "";
        ClientConnectionInfo res = new ClientConnectionInfo(connectionSocket, outToClient);

        fileName = tokenizedLine.nextToken();

        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        System.out.println(ConsoleColors.BLUE+"HTTP GET "+fileName+ConsoleColors.RESET);


        if (mServer.getMemory().containsKey(fileName)) {
            VirtualFile requestedFile = mServer.getMemory().get(fileName);
            System.out.println(ConsoleColors.GREEN+"File Exists"+ConsoleColors.RESET);
            outToClient.writeBytes("HTTP/1.1 200 Document Follows\r\n");
            if (fileName.endsWith(".jpg"))
                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
            if (fileName.endsWith(".gif"))
                outToClient.writeBytes("Content-Type: image/gif\r\n");
            outToClient.writeBytes("Content-Length: " + requestedFile.getContent().length() + "\r\n");
            outToClient.writeBytes("\r\n");


            outToClient.write(requestedFile.getContent().getBytes(), 0, requestedFile.getContent().length());
            connectionSocket.close();
        } else {
            System.out.println(ConsoleColors.RED+"File NOT Found"+ConsoleColors.RESET);

            if(pendingClientGetReq.containsKey(fileName)){ // another client has already requested this file
                // so just make this client to wait for the answer
                pendingClientGetReq.get(fileName).add(res);
                // don't make another request
            }else {
                if (mServer.getNextServer().equals(mServer.getThisServer())) {//Means that server has no siblings
                    outToClient.writeBytes("HTTP/1.1 404 File Not Found\r\n");
                    connectionSocket.close();
                }else {
                    ArrayList<ClientConnectionInfo> tmp = new ArrayList<>();
                    tmp.add(res);
                    pendingClientGetReq.put(fileName,tmp);
                    // not logical right cauz file doesn't actually exists
                    MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, new VirtualFile(fileName, ""));
                    mm.start();
                }
            }

        }

    }

    @Override
    public void run() {
        System.out.println("Running " + threadName);
        try {
            ServerSocket listenSocket = new ServerSocket(port);
            System.out.println(ConsoleColors.GREEN+"Server ready on " + port+ConsoleColors.RESET);
            String requestMessageLine;
            while (true) {

                Socket connectionSocket = listenSocket.accept();
                BufferedReader inFromMember = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                DataOutputStream outToMember = new DataOutputStream(connectionSocket.getOutputStream());
                requestMessageLine = inFromMember.readLine();
                if (requestMessageLine != null && requestMessageLine.startsWith("ELEF")) {
                    requestMessageLine = inFromMember.readLine();
                    System.out.println(ConsoleColors.BLUE + requestMessageLine + ConsoleColors.RESET);
                    if (requestMessageLine != null && requestMessageLine.startsWith("JOIN")) {
                        int diff = mServer.getGroupMembers().size() - mServer.getIndexAsMember();
                        outToMember.writeBytes(String.valueOf(diff) + '\n');
                        if (diff == 1) {
                            //server doens't have right sibling
                            outToMember.writeBytes(mServer.groupMembersToString() + '\n');
                            GroupMember gm = GroupMember.parseGroupMember(inFromMember.readLine()); // name of other member
                            mServer.getGroupMembers().add(gm);
                            mServer.showGroupMembers();
                        } else {
                            //server HAS right sibling
                            int indexToAdd = mServer.getIndexAsMember();
                            outToMember.writeBytes(mServer.groupMembersToString() + '\n');
                            GroupMember gm = GroupMember.parseGroupMember(inFromMember.readLine()); // name of other member
                            mServer.getGroupMembers().add(indexToAdd + 1, gm);
                            mServer.showGroupMembers();
                        }
                        connectionSocket.close();
                    } else if (requestMessageLine != null && requestMessageLine.startsWith("STATUS")) {
                        String memberLists = inFromMember.readLine();
                        if (memberLists.contains(mServer.getName())) {
                            System.out.println(ConsoleColors.GREEN+ "-- STATUS Response" + ConsoleColors.RESET);
                            ArrayList<GroupMember> gms = GroupMember.parseGroupMembers(memberLists);
                            mServer.setGroupMembers(gms);
                            mServer.showGroupMembers();
                        } else {
                            StatusMonitor.sendReq(memberLists, mServer,mServer.getNextServer());
                        }
                        connectionSocket.close();
                    } else if (requestMessageLine != null && requestMessageLine.startsWith("FILE PUT")) {
                        String filenameLine = inFromMember.readLine();
                        String contentLine = inFromMember.readLine();
                        String timestampLine = inFromMember.readLine();
                        String senderLine = inFromMember.readLine();

                        String[] fileSplit = filenameLine.split("[:]");
                        String[] contentSplit = contentLine.split("[:]");
                        String[] timestampSplit = timestampLine.split("[:]");

                        String filename = fileSplit[1];
                        String content = contentSplit[1];
                        String timestamp = timestampSplit[1];

                        Long tm = Long.parseLong(timestamp);
//                        Instant i = Instant.ofEpochMilli(tm);
                        VirtualFile vf = new VirtualFile(filename, content, tm);
                        mServer.storeFile(vf);

                        if (senderLine.contains(mServer.getName())) {
                            System.out.println(ConsoleColors.GREEN+filename + " stored to group !!"+ConsoleColors.RESET);
                            System.out.println(ConsoleColors.BLUE+"Answering to client"+ConsoleColors.RESET);
                            Socket toBeClosed = pendingClientPutReq.get(tm).getOpenedSocket();
                            DataOutputStream out = pendingClientPutReq.get(tm).getOutStream();

                            out.writeBytes("HTTP/1.1 201 Created\r\n");

                            if (filename.endsWith(".jpg"))
                                out.writeBytes("Content-Type: image/jpeg\r\n");
                            if (filename.endsWith(".gif"))
                                out.writeBytes("Content-Type: image/gif\r\n");

                            out.writeBytes("Content-Location: /" + filename + "\r\n");
                            toBeClosed.close();
                            pendingClientPutReq.remove(vf.getTimestamp());
                        } else {
                            MemoryMonitor mm = new MemoryMonitor("Listening" + mServer.getName(), MemoryMonitor.ReqType.PUT, mServer, vf);
                            mm.sendFileRequest(senderLine, mServer.getNextServer());
                        }


                        connectionSocket.close();
                    } else if (requestMessageLine != null && requestMessageLine.startsWith("FILE GET")) {
                        String filenameLine = inFromMember.readLine();
                        String[] fileSplit = filenameLine.split("[:]");
                        String filename = fileSplit[1];
                        if (mServer.getMemory().containsKey(filename)) {
                            System.out.println(mServer.getName() + " has the file");
                            String buffer = "";

                            // update other servers
                            String senderLine = inFromMember.readLine();
                            if(!senderLine.contains("SENDER")){ //maybe someone else had the file
                                senderLine = inFromMember.readLine();
                                senderLine = inFromMember.readLine();
                            }
                            String contentLine = "CONTENT:" + mServer.getMemory().get(filename).getContent() + "\n";
                            String timestampLine = "TIMESTAMP:" + mServer.getMemory().get(filename).getTimestamp() + "\n";
                            buffer = buffer.concat(contentLine);
                            buffer = buffer.concat(timestampLine);
                            buffer = buffer.concat(senderLine);

                            if(senderLine.contains(mServer.getName())){

                                System.out.println(ConsoleColors.BLUE+"FILE GET Cirlce Complete"+ConsoleColors.RESET);
                                System.out.println(ConsoleColors.GREEN+"Answering to client"+ConsoleColors.RESET);

                                answerGETToClient(filename);
                            }else{
                                MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, new VirtualFile(filename, ""), buffer);
                                mm.start();
                            }



                        } else {
                            System.out.println(ConsoleColors.RED+filename +" not found in "+mServer.getName()+ConsoleColors.RESET);
                            String line = inFromMember.readLine();
                            String buffer = "";
                            VirtualFile vf = null;
                            if (line.contains("SENDER")) { //means that no valuable info about this file exists and  this is the end of this message

                                if (line.contains(mServer.getName())) { // circle has been completed
                                    System.out.println(ConsoleColors.BLUE+"FILE GET Cirlce Complete"+ConsoleColors.RESET);
                                    System.out.println(ConsoleColors.GREEN+"Answering to client"+ConsoleColors.RESET);
                                    //close connection

                                    answerGETToClient(filename);
                                } else {// forward message
                                    MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, new VirtualFile(filename, ""), line);
                                    mm.start();
                                }

                            } else { // there is valuable info about the file and server should save it
                                //then line should contain CONTENT
                                buffer = buffer.concat(line+"\n");
                                String[] contentSplit = line.split("[:]");
                                line = inFromMember.readLine(); // TIMESTAMP
                                buffer = buffer.concat(line+"\n");
                                String[] timestampSplit = line.split("[:]");

                                String senderLine = inFromMember.readLine(); // SENDER
                                buffer = buffer.concat(senderLine+"\n");

                                String content = contentSplit[1];
                                String timestamp = timestampSplit[1];

                                long tm = Long.parseLong(timestamp);
//                                Instant i = Instant.ofEpochMilli(tm);
                                vf = new VirtualFile(filename, content, tm);
                                mServer.storeFile(vf);

                                if(senderLine.contains(mServer.getName())){
                                    System.out.println(ConsoleColors.BLUE+"FILE GET Circle Complete"+ConsoleColors.RESET);
                                    System.out.println(ConsoleColors.GREEN+"Answering to client"+ConsoleColors.RESET);

                                    answerGETToClient(filename);
                                }else{
                                    System.out.println(ConsoleColors.GREEN+"Forwarding this msg: \n"+buffer+ConsoleColors.RESET);
                                    MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, vf, buffer);
                                    mm.start();
                                }
                            }

                        }
                        connectionSocket.close();
                    }

                } else if (requestMessageLine != null && (requestMessageLine.startsWith("GET") || requestMessageLine.startsWith("PUT"))) {
                    StringTokenizer tokenizedLine = new StringTokenizer(requestMessageLine);
                    String firstToken = tokenizedLine.nextToken();

                    if (firstToken.equals("GET")) {

                        handleClientGET(connectionSocket,tokenizedLine);

                    } else if (firstToken.equals("PUT")) {

                        handleClientPUT(connectionSocket,tokenizedLine);
                    }
                } else if (requestMessageLine != null && requestMessageLine.startsWith("EXIT")) {
                    System.out.println("Leaving");
                    connectionSocket.close();
                    break;
                } else {
                    System.out.println("[404] Request not Found :(");
                    connectionSocket.close();
                }
            }
            listenSocket.close();

        } catch (SocketException e) {
            System.out.println("Probably a peer or client has disconnected or port is already being used ");
        } catch (IOException e) {
            System.out.println("Cannot listen on port: " + port);
            e.printStackTrace();
        }
    }
}
