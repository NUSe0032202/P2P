import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class DirectoryServerThread extends Thread {
    public static final String EXIT_MESSAGE = "EXIT";
    public static final String QUERY_MESSAGE = "QUERY_FILE";
    public static final String LIST_MESSAGE = "LIST_ALL_FILES";
    public static final String UPDATE_MESSAGE = "UPDATE";
    public static final int SIZE_OF_CHUNK = 100;//Size is in bytes wise

    private static final Peer NON_EXISTENT_PEER = new Peer("NEP", "NEP", -1, "NEP", -1);

    private Socket socket = null;
    private int threadNumber;
    private DirectoryServerListen dirServer;
    private String cmd;
    private boolean isConnecting;

    private static String hostname, publicIPAddress,privateIPAddress;
    private static int publicPort,privatePort;
    private static Peer curPeer;

    public DirectoryServerThread(Socket socket, int currNo, DirectoryServerListen dirServer) {
        super("DirectoryServerThread" + currNo);
        this.socket = socket;
        this.threadNumber = currNo;
        this.dirServer = dirServer;
        this.cmd = null;
        this.isConnecting = false;
        System.out.println("Connection from: " + socket.getRemoteSocketAddress().toString());
    }

    public void run() {
        try {
            isConnecting = true;
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (isConnecting) {
                cmd = in.readLine();
                String[] args = cmd.split("#");
                switch (args[0]) {
                    case UPDATE_MESSAGE:
                        System.out.println(cmd);
                        int e = Integer.parseInt(args[5]);
                        hostname = "host@IP(Pub): " + args[1] + " P(Pub): " + args[2] + " IP(Pvt): " +args[3] + " P(Pvt): " + args[4]; //host@192.168.1.1 for example
                        publicIPAddress = args[1]; //IP is argument 2
                        publicPort = Integer.parseInt(args[2]); //Port is argument 3
                        privateIPAddress = args[3];
                        privatePort = Integer.parseInt(args[4]);
                        curPeer = new Peer(hostname, publicIPAddress, publicPort, privateIPAddress, privatePort);//Create the peer
                        for(int i = 0 ; i<e;i++) {
                            ArrayList<Chunk> availChunks = new ArrayList<Chunk>();
                            String [] fileArgs = args[5 + i].split(",");
                            for(int a = 0 ;a<Integer.parseInt(fileArgs[2]);a++) {//File Size needed here
                                if(Integer.parseInt(fileArgs[2+a+1]) == 1)
                                    availChunks.add(new Chunk(new FileEntry(fileArgs[0], Integer.parseInt(fileArgs[1]),SIZE_OF_CHUNK), a+1)); // dummy value
                            }
                            dirServer.update(curPeer, availChunks);
                        }
                        out.println("Processed a total of " + e + "entries");
                        out.println("EOP");
                        break;
                    case LIST_MESSAGE:
                        int noFiles = 0;
                        ArrayList<FileEntry> availFiles = dirServer.listAllFiles();
                        for (FileEntry file : availFiles) {
                            out.println((noFiles += 1)+". Filename: " + file.getFilename() + "  File size: " + file.getFileSize() + " Bytes");
                        }
                        out.println("EOP");
                        break;
                    case QUERY_MESSAGE:
                        System.out.println(cmd);
                        String toSend = "";
                        String filename = args[1];
                        FileEntry file = dirServer.getFilenameToFileEntry().get(filename);
                        ArrayList<Peer> availPeers = new ArrayList();
                        for (int chunkIndex = 0; chunkIndex < file.getNumChunks(); chunkIndex++) {
                            Chunk requestChunk = new Chunk(file, chunkIndex + 1);//Chunks number stating from 1 onwards
                            ArrayList<Peer> availPeer = dirServer.getChunk(requestChunk);//From Chunks to Peers mapping
                            if (availPeer.isEmpty()) {
                                availPeers.add(NON_EXISTENT_PEER);
                            } else {
                                availPeers.add(availPeer.get(0));//Return the first peer of who has that chunk, to be improved by randomization
                            }
                        }
                        toSend += "D#";//Data output starts after here
                        toSend += String.valueOf(file.getNumChunks());//1) First data is number of chunks making up the file,no of chunks per file
                        toSend += ",";
                        toSend += String.valueOf(file.getFileSize());//File Size
                        toSend += ",";
                        for (Peer peer : availPeers) {
                            toSend += peer.getPublicIpAddress() + " " + String.valueOf(peer.getPublicPortNumber() + " " + peer.getPrivateIpAddress() + " " + String.valueOf(peer.getPrivatePortNumber()));//2) IP SPACE PORT
                            toSend += ",";
                        }
                        out.println(toSend);//3) Send the complete data in a string back
                        System.out.println(toSend);
                        out.println("EOP");
                        break;
                    case EXIT_MESSAGE:
                        out.println("Exit Message Received");//One stirng For I/O//Delete
                        socket.close();
                        isConnecting = false;
                        publicIPAddress = args[1];
                        publicPort = Integer.valueOf(args[2]);
                        privateIPAddress = args[3];
                        privatePort = Integer.valueOf(args[4]);
                        curPeer = new Peer(publicIPAddress,publicPort,privateIPAddress,privatePort);
                        //Comparison between two peers is done comparing their IPs & Ports only hostname is not considered now
                        dirServer.removePeer(curPeer);
                        break;
                }
            }
        } catch (IOException exception) {
            System.out.println(exception);
        }
    }
}
