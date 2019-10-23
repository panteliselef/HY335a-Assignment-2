import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;


public class ListenMonitor extends Thread {
    private String threadName;
    private int port;
    private WebServer mServer;
    private HashMap<Long, HttpPutResponse> pendingClientReq = new HashMap<>();

    ListenMonitor(String threadName, int port, WebServer mServer) {
        this.threadName = "[Listen Monitor]" + threadName;
        this.port = port;
        this.mServer = mServer;
        System.out.println("Listening:  " + threadName);
    }

    private void sendMemberList(DataOutputStream out) throws IOException {
        String msg = "";
        for (int i = 0; i < mServer.getGroupMembers().size(); i++) {
            if (i != mServer.getGroupMembers().size()) {
                msg = msg.concat(mServer.getGroupMembers().get(i).getName() + "," + mServer.getGroupMembers().get(i).getPort() + "," + mServer.getGroupMembers().get(i).getIpAddress() + "$");
            } else {
                msg = msg.concat(mServer.getGroupMembers().get(i).getName() + "," + mServer.getGroupMembers().get(i).getPort() + "," + mServer.getGroupMembers().get(i).getIpAddress());
            }
        }
        System.out.println(msg);
        out.writeBytes(msg + '\n');
    }


    private GroupMember getNewMember(String lineFromServer) {
        String[] info = lineFromServer.split("[,]");
        GroupMember gm = new GroupMember(info[0], Integer.parseInt(info[1]), info[2]);
        return gm;
    }

    private ArrayList<GroupMember> parseMembers(String lineFromServer) {
        String[] items = lineFromServer.split("[$]");
        ArrayList<GroupMember> gms = new ArrayList<>();
        for (String item : items) {
            String[] info = item.split("[,]");
            GroupMember gm = new GroupMember(info[0], Integer.parseInt(info[1]), info[2]);
            gms.add(gm);
        }
        return gms;
    }

    @Override
    public void run() {
        System.out.println("Running " + threadName);
        try {
            ServerSocket listenSocket = new ServerSocket(port);
            System.out.println("Server ready on " + port);
            String requestMessageLine;
            while (true) {

                Socket connectionSocket = listenSocket.accept();
                BufferedReader inFromMember = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                DataOutputStream outToMember = new DataOutputStream(connectionSocket.getOutputStream());
                requestMessageLine = inFromMember.readLine();
                System.out.println(requestMessageLine);
                if (requestMessageLine != null && requestMessageLine.startsWith("ELEF")) {
                    requestMessageLine = inFromMember.readLine();
                    System.out.println(requestMessageLine);
                    if (requestMessageLine != null && requestMessageLine.startsWith("JOIN")) {
                        int diff = mServer.getGroupMembers().size() - mServer.getIndexAsMember();
                        outToMember.writeBytes(String.valueOf(diff) + '\n');
                        if (diff == 1) {
                            //server doens't have right sibling
                            sendMemberList(outToMember);
                            GroupMember gm = getNewMember(inFromMember.readLine()); // name of other member
                            mServer.getGroupMembers().add(gm);
                            mServer.showGroupMembers();
                        } else {
                            //server HAS right sibling
                            int indexToAdd = mServer.getIndexAsMember();
                            sendMemberList(outToMember);
                            GroupMember gm = getNewMember(inFromMember.readLine()); // name of other member
                            mServer.getGroupMembers().add(indexToAdd + 1, gm);
                            mServer.showGroupMembers();
                        }
                        connectionSocket.close();
                    } else if (requestMessageLine != null && requestMessageLine.startsWith("STATUS")) {
                        String memberLists = inFromMember.readLine();
                        if (memberLists.contains(mServer.getName())) {
                            System.out.println("Cycle has been completed");
                            ArrayList<GroupMember> gms = parseMembers(memberLists);
                            mServer.setGroupMembers(gms);
                            mServer.showGroupMembers();
                        } else {
                            StatusMonitor.sendReq(memberLists, mServer);
                        }
//                        GroupMember gm = getNewMember(inFromMember.readLine());
//                        System.out.println(gm);
                        connectionSocket.close();
                    } else if (requestMessageLine != null && requestMessageLine.startsWith("FILE PUT")) {
                        String filenameLine = inFromMember.readLine();
                        String contentLine = inFromMember.readLine();
                        String timestampLine = inFromMember.readLine();
                        String senderLine = inFromMember.readLine();

                        System.out.println("----");
                        String[] fileSplit = filenameLine.split("[:]");
                        String[] contentSplit = contentLine.split("[:]");
                        String[] timestampSplit = timestampLine.split("[:]");

                        String filename = fileSplit[1];
                        String content = contentSplit[1];
                        String timestamp = timestampSplit[1];

                        Long tm = Long.parseLong(timestamp);
                        Instant i = Instant.ofEpochMilli(tm);
                        VirtualFile vf = new VirtualFile(filename, content, i);
                        mServer.storeFile(vf);

                        if (senderLine.contains(mServer.getName())) {
                            System.out.println(filename + " has been stored to group!!");
                            System.out.println("Answering to client");
                            Socket toBeClosed = pendingClientReq.get(tm).getOpenedSocket();
                            DataOutputStream out = pendingClientReq.get(tm).getOutStream();

                            out.writeBytes("HTTP/1.1 201 Created\r\n");

                            if (filename.endsWith(".jpg"))
                                out.writeBytes("Content-Type: image/jpeg\r\n");
                            if (filename.endsWith(".gif"))
                                out.writeBytes("Content-Type: image/gif\r\n");

                            out.writeBytes("Content-Location: /" + filename + "\r\n");
                            toBeClosed.close();
                            pendingClientReq.remove(vf.getTimestamp().toEpochMilli());
                        } else {
                            MemoryMonitor mm = new MemoryMonitor("Listening" + mServer.getName(), MemoryMonitor.ReqType.PUT, mServer, vf);
                            mm.sendFileRequest(senderLine, mServer.getNextServer());
                        }


                        connectionSocket.close();
                    } else if (requestMessageLine != null && requestMessageLine.startsWith("FILE GET")) {
                        if (requestMessageLine.contains(mServer.getName())) {// means that a circle has been completed
                            System.out.println("FILE GET Cirle Complete");
                        }
                        String filenameLine = inFromMember.readLine();
                        String[] fileSplit = filenameLine.split("[:]");
                        String filename = fileSplit[1];
                        if (mServer.getMemory().containsKey(filename)) {
                            System.out.println(mServer.getName() + " has the file");
                            String buffer = "";
                            // update other servers
                            String senderLine = inFromMember.readLine();
                            String contentLine = "CONTENT:" + mServer.getMemory().get(filename).getContent() + "\n";
                            String timestampLine = "TIMESTAMP:" + mServer.getMemory().get(filename).getTimestamp().toEpochMilli() + "\n";
                            buffer = buffer.concat(contentLine);
                            buffer = buffer.concat(timestampLine);
                            buffer = buffer.concat(senderLine);

                            if(senderLine.contains(mServer.getName())){
                                System.out.println("FILE GET Cirlce Complete");
                            }else{
                                MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, new VirtualFile(filename, ""), buffer);
                                mm.start();
                            }



                        } else {
                            System.out.println(mServer.getName() + " does not have the file");

                            String line = inFromMember.readLine();
                            String buffer = "";
                            VirtualFile vf = null;
                            if (line.contains("SENDER")) { //means that no valuable info about this file exists and  this is the end of this message
                                System.out.println(mServer.getName() + " no file to save");

                                if (line.contains(mServer.getName())) { // circle has been completed
                                    System.out.println("FILE GET Cirlce Complete");
                                    //close connection
                                } else {// forward message
                                    MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, new VirtualFile(filename, ""), line);
                                    mm.start();
                                }

                            } else { // there is valuable info about the file and server should save it
                                //then line should contain CONTENT
                                System.out.println(mServer.getName() + " saving the file");
                                buffer = buffer.concat(line);
                                System.out.println("content -> " + line);
                                String[] contentSplit = line.split("[:]");
                                line = inFromMember.readLine(); // TIMESTAMP
                                System.out.println("timestamp -> " + line);
                                buffer = buffer.concat(line);
                                String[] timestampSplit = line.split("[:]");

                                String senderLine = inFromMember.readLine(); // SENDER
                                buffer = buffer.concat(senderLine);

                                String content = contentSplit[1];
                                String timestamp = timestampSplit[1];
                                System.out.println("t:" + timestamp);

                                long tm = Long.parseLong(timestamp);
                                Instant i = Instant.ofEpochMilli(tm);
                                vf = new VirtualFile(filename, content, i);
                                mServer.storeFile(vf);

                                if(senderLine.contains(mServer.getName())){
                                    System.out.println("FILE GET Circle Complete");
                                }else{
                                    MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, vf, buffer);
                                    mm.start();
                                }
                            }

                            // just forward the message to the group

                        }
                        System.out.println("----");
                        System.out.println(filenameLine);
                        connectionSocket.close();
                    }

                } else if (requestMessageLine != null && (requestMessageLine.startsWith("GET") || requestMessageLine.startsWith("PUT"))) {

                    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                    DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

                    StringTokenizer tokenizedLine = new StringTokenizer(requestMessageLine);
                    String firstToken = tokenizedLine.nextToken();

                    if (firstToken.equals("GET")) {
                        String fileName = "";

                        fileName = tokenizedLine.nextToken();

//                        System.out.println(firstToken + " " + fileName);
                        if (fileName.startsWith("/")) {
                            fileName = fileName.substring(1);

                        }

                        int numOfBytes = 0;

                        if (mServer.getMemory().containsKey(fileName)) {
                            VirtualFile requestedFile = mServer.getMemory().get(fileName);
                            System.out.println("File Exists");
                            outToClient.writeBytes("HTTP/1.1 200 Document Follows\r\n");
                            if (fileName.endsWith(".jpg"))
                                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
                            if (fileName.endsWith(".gif"))
                                outToClient.writeBytes("Content-Type: image/gif\r\n");
                            outToClient.writeBytes("Content-Length: " + requestedFile.getContent().length() + "\r\n");
                            outToClient.writeBytes("\r\n");


                            outToClient.write(requestedFile.getContent().getBytes(), 0, requestedFile.getContent().length());
                        } else {

                            System.out.println("File NOT Exists");

                            // not logical right cauz file doesn't actually exists
                            MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.GET, mServer, new VirtualFile(fileName, ""));
                            mm.start();
                        }
//                        try {
//                            File file = new File(fileName);
//                            numOfBytes = (int) file.length();
//
//                            FileInputStream inFile = new FileInputStream(fileName);
//                            byte[] fileInBytes = new byte[numOfBytes];
//                            inFile.read(fileInBytes);
//                            inFile.close();
//
//                            outToClient.writeBytes("HTTP/1.1 200 Document Follows\r\n");
//
//                            if (fileName.endsWith(".jpg"))
//                                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
//                            if (fileName.endsWith(".gif"))
//                                outToClient.writeBytes("Content-Type: image/gif\r\n");
//
//                            outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
//                            outToClient.writeBytes("\r\n");
//
//                            outToClient.write(fileInBytes, 0, numOfBytes);
//                        } catch (FileNotFoundException e) {
//                            System.out.println("Unable to locate: /" + fileName);
//                        }
                        connectionSocket.close();
                    } else if (firstToken.equals("PUT")) {
                        String fileName = "";
                        HttpPutResponse res = new HttpPutResponse(connectionSocket, outToClient);
                        fileName = tokenizedLine.nextToken();

                        System.out.println(firstToken + " " + fileName);

                        if (fileName.startsWith("/")) {
                            fileName = fileName.substring(1);
                        }

                        requestMessageLine = inFromClient.readLine(); // Content-Type
                        requestMessageLine = inFromClient.readLine(); // Content-length
                        StringTokenizer tok = new StringTokenizer(requestMessageLine);
                        tok.nextToken();
                        int numOfBytes = Integer.parseInt(tok.nextToken());
                        char[] content = new char[numOfBytes];

//                        byte[] contentInBytes = new byte[numOfBytes];
                        inFromClient.read(content); // read bytes
                        VirtualFile vf = new VirtualFile(fileName, String.copyValueOf(content));
                        pendingClientReq.put(vf.getTimestamp().toEpochMilli(), res);
                        MemoryMonitor mm = new MemoryMonitor(mServer.getName(), MemoryMonitor.ReqType.PUT, mServer, vf);
                        mm.start();
                        mServer.getMemory().put(fileName, vf);
                        mServer.showMemomry();


                        if (mServer.getGroupMembers().size() == 1) {
                            outToClient.writeBytes("HTTP/1.1 201 Created\r\n");

                            if (fileName.endsWith(".jpg"))
                                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
                            if (fileName.endsWith(".gif"))
                                outToClient.writeBytes("Content-Type: image/gif\r\n");

                            outToClient.writeBytes("Content-Location: /" + fileName + "\r\n");
                            pendingClientReq.remove(vf.getTimestamp().toEpochMilli());
                            connectionSocket.close();
                        }

                    }
//                    connectionSocket.close();
                } else if (requestMessageLine != null && requestMessageLine.startsWith("EXIT")) {
                    System.out.println("Leaving");
                    connectionSocket.close();
                    break;
                } else {
                    System.out.println("[404] Request not Found :(");
                    connectionSocket.close();
                }
//                connectionSocket.close();
            }
            listenSocket.close();

        } catch (SocketException e) {
            System.out.println("Probably a peer or client has disconnected or port is already being used ");
//            run();
//            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Cannot listen on port: " + port);
            e.printStackTrace();
        }
    }
}
