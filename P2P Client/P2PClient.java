import java.io.*;
import java.net.*;
import java.util.*;

public class P2PClient {
    // GUI Input and outputs
    public static Scanner sc;
    public static PrintStream ps = System.out;

    // IP and Port settings
    private static String publicIP;
    private static Socket relayConnect;
    private static int publicPort;
    private static int listenerPort;
    private static final int RELAY_PORT = 3003;
    private static final int CLIENT_LISTENER_PORT = 3001;
    private static final int SEND = 4000;
    private static final int RECV = 13335;

    // Message constants
    public static final int DEFAULT_CHUNKSIZE = 50000;
    public static final String DELIMITER = "#";
    public static final String UPDATE_DELIMITER = ",";
    public static final String EXIT_MESSAGE = "EXIT" + DELIMITER;
    public static final String QUERY_MESSAGE = "QUERY_FILE" + DELIMITER;
    public static final String LIST_MESSAGE = "LIST_ALL_FILES" + DELIMITER;
    public static final String CONNECT_MESSAGE = "CONNECT" + DELIMITER;
    public static final String UPDATE_MESSAGE = "UPDATE" + DELIMITER;

    // Initialise File Directory instance
    public static FileDirectory fileDir = new FileDirectory();

    // Program main class
    public static void main(String[] args) {
        // Initialise session instances and variables
        Socket socket = null;
        PrintWriter oos = null;
        BufferedReader ois = null;
        boolean connectedToDirectoryServer = false;
        int selectedOperation = 0;
        sc = new Scanner(System.in);

        CustomPeer temp = getPublicIPnPort("172.25.99.145", RECV);
        publicIP = temp.getIP();
        publicPort = temp.getPort();

        try {
            while (true) {
                // If connected to the directory server,
                if (connectedToDirectoryServer) {
                    // Displays menu UI and waits for desired operation from user
                    selectedOperation = displayMenu();

                    switch (selectedOperation) {
                        // LIST_ALL_FILES: List all the files in the P2P network
                        case 1:
                            // Sends the LIST_ALL_FILES command to directory server
                            oos.println(LIST_MESSAGE);

                            // Prints reply from directory server
                            ps.println("Files available: ");
                            printServerReply(ois);
                            break;

                        // UPDATE: Manual update
                        case 2:
                            List<FileInfo> currFileList = fileDir.getFiles();

                            // Constructs the message to send to directory server
                            String updateMsg = "";
                            updateMsg += UPDATE_MESSAGE;                                    // UPDATE flag
                            updateMsg += publicIP + DELIMITER;                              // Public IP (P2P client)
                            updateMsg += publicPort + DELIMITER;                            // Listening port (P2P client)
                            updateMsg += "172.25.99.145" + DELIMITER;                     // Own IP address
                            updateMsg += CLIENT_LISTENER_PORT + DELIMITER;                  // Private port(Listener)
                            updateMsg += String.valueOf(currFileList.size()) + DELIMITER;   // Number of files to expect

                            // For each file in own client's FileDirectory, append to updateMsg the following:
                            for (int i = 0; i < currFileList.size(); i++) {
                                // Information of each file (file name, file size, number of constituent chunks)
                                updateMsg += currFileList.get(i).getFilename() + UPDATE_DELIMITER;
                                updateMsg += Integer.toString(currFileList.get(i).getFileSize()) + UPDATE_DELIMITER;
                                updateMsg += Integer.toString(currFileList.get(i).getNumChunks()) + UPDATE_DELIMITER;

                                // Chunk map of each file
                                boolean[] tempFlag = fileDir.getFiles().get(i).getChunkAvailabilityMap();
                                for (int j = 0; j < tempFlag.length; j++) {
                                    if (tempFlag[j] == true)    // If client has chunk, mark as 1
                                        updateMsg += "1" + UPDATE_DELIMITER;
                                    else                        // If client doesn't have chunk, mark as 0
                                        updateMsg += "0" + UPDATE_DELIMITER;
                                }
                                updateMsg += DELIMITER;
                            }

                            // Sends the compiled message to directory server
                            oos.println(updateMsg);

                            // Prints reply from directory server
                            // printServerReply(ois);
                            ps.println("Message sent to server: '"+updateMsg+"'.");

                            break;

                        // QUERY_FILE: Download a particular file
                        case 3:
                            String fileName, incomingData = null;
                            Peer currChunk;

                            // Obtain name of desired file from user
                            ps.print("Please enter the name of the file to download: ");
                            fileName = sc.nextLine();

                            // Sends QUERY_FILE request to directory server
                            oos.println(QUERY_MESSAGE + fileName);

                            // Receives FileInfo of file from directory server and adds it into local FileDirectory
                            readData(ois, fileName);
                            FileInfo fileInfo = fileDir.getFileInfoByFileName(fileName);
                            System.out.println("Total number of chunks in " + fileName + ": " + fileInfo.getNumChunks());

                            DataOutputStream fileWriter = new DataOutputStream(new FileOutputStream(fileName));

                            for (int chunkNum = 1; chunkNum < fileInfo.getNumChunks() + 1; chunkNum++) {
                                currChunk = fileInfo.getChunkInfo(chunkNum);
                                System.out.print("Attempting to download chunk #" + chunkNum);
                                
                                // If is public IP
                                if (publicIP.equals(currChunk.getPublicIP())) { //178.128.118.155
                                    System.out.print(" directly from peer's private IP " + publicIP + "... ");
                                    // Create socket to peer with chunk and set up writers and readers
                                    Socket senderSocket = new Socket(currChunk.getPrivateIP(), currChunk.getPrivatePort());
                                    PrintWriter requestWriter = new PrintWriter(senderSocket.getOutputStream(), true);
                                    DataInputStream reader = new DataInputStream(new BufferedInputStream(senderSocket.getInputStream()));

                                    // Send the desired file + chunk to the peer
                                    requestWriter.println(fileName + "," + String.valueOf(chunkNum));

                                    // Data transfer starts here
                                    byte[] buffer = new byte[DEFAULT_CHUNKSIZE];
                                    int bytesRead = 0, bytesCounter = 0;
                                    while (true) {
                                        bytesRead = reader.read(buffer, 0, DEFAULT_CHUNKSIZE - bytesCounter);
                                        if (bytesRead == -1 || bytesCounter==DEFAULT_CHUNKSIZE) {
                                            break;
                                        } else {
                                            bytesCounter += bytesRead;
                                            // ps.println("Read " + bytesRead + " bytes...");
                                        }
                                        fileWriter.write(buffer, 0, bytesRead);
                                    }

                                    // Marks chunk as available on local FileDirectory
                                    boolean[] ptr = fileDir.getFileInfoByFileName(fileName).getChunkAvailabilityMap();
                                    ptr[chunkNum - 1] = true;

                                    ps.println("complete! (downloaded " + bytesCounter + " bytes).");
                                
                                // If is private IP
                                } else {
                                    System.out.println("from peer's public IP. Attempting to resolve NAT...");

                                    // UDP hole punching
                                    udpHolePunching(currChunk.getPublicIP(), currChunk.getPublicPort(), RECV);
                                    
                                    DatagramSocket recvSocket = new DatagramSocket(RECV);
                                    PrintWriter relayWriter = new PrintWriter(relayConnect.getOutputStream(), true);
                                    
                                    relayWriter.println("Request start");
                                    relayWriter.println(fileName + ","+chunkNum + "," + currChunk.getPublicIP() + "," + String.valueOf(currChunk.getPublicPort()) + "," + publicIP + "," + RECV);

                                    // Reads chunk datagram and writes to file
                                    byte[] buffer = new byte[DEFAULT_CHUNKSIZE];
                                    DatagramPacket receivePacket = new DatagramPacket(buffer,DEFAULT_CHUNKSIZE);
                                    recvSocket.receive(receivePacket);
                                    fileWriter.write(buffer,0,DEFAULT_CHUNKSIZE);

                                    // Marks chunk as available on local FileDirectory
                                    boolean[] ptr = fileDir.getFileInfoByFileName(fileName).getChunkAvailabilityMap();
                                    ptr[chunkNum - 1] = true;

                                    ps.println("Downloaded chunk #" + chunkNum + " of " + fileName + ".");
                                    recvSocket.close();
                                }
                            }
                            ps.println("Successfully downloaded " + fileName + " (total chunks: " + fileInfo.getNumChunks() + ")!");
                            fileWriter.close();
                            break;
                            
                        // EXIT: Exit the P2P network and disconnect from directory server
                        case 4:
                            // Constructs the message to send to directory server
                            String exitMsg = "";
                            exitMsg += EXIT_MESSAGE;                    // EXIT flag
                            exitMsg += getOwnPrivateIP() + DELIMITER;   // Private IP
                            exitMsg += CLIENT_LISTENER_PORT + DELIMITER;// Private port (Listener)
                            exitMsg += publicIP + DELIMITER;            // Public IP
                            exitMsg += listenerPort + DELIMITER;        // Public port (Listener)

                            // Sends EXIT request to directory server
                            ps.println("Closed connection to directory server.");
                            oos.println(exitMsg);
                            
                            // Closes sockets
                            socket.close();
                            connectedToDirectoryServer = false;
                            break;

                        default:
                            ps.println("An invalid option was selected. Please try again.");
                    }
                
                // If not connected to directory server,
                } else {
                    // Establish a connection to the directory server
                    try {
                        socket = getDirectoryServerSocket();
                        connectedToDirectoryServer = true;
                        oos = new PrintWriter(socket.getOutputStream(), true);
                        ois = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        ps.println("Successfully established connection to directory server.");

                        // Run the client listener
                        new Listener(CLIENT_LISTENER_PORT).start();

                        // Open persistent connection to relay server
                        relayConnect = new Socket("178.128.118.155",5001);
                        PrintWriter out_2 = new PrintWriter(relayConnect.getOutputStream(), true);
                        out_2.println(String.valueOf(SEND));
                        new NATListen(relayConnect,SEND).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        ps.println("Connection to the specified server was unsuccessful. Please try again.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Returns the private ip address of the WIFI adapter that this client is running on.
    public static String getOwnPrivateIP() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    if (addr instanceof Inet6Address) continue;

                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ip;
    }
    
    // Displays menu and obtains user choice of operation
    public static int displayMenu() {
        // Prints menu
        ps.print("=========================================" +
                "\nWelcome to CS3103's File Transfer Program" +
                "\n=========================================" +
                "\n1. List all available files" +
                "\n2. Send update to directory server" +
                "\n3. Download file" +
                "\n4. Exit" +
                "\nPlease select an option (1-4): ");
        return Integer.parseInt(sc.nextLine());
    }

    // Connect to the directory server
    public static Socket getDirectoryServerSocket() throws IOException {
        String ip, port;
//        // Gets IP of directory server
//        ps.println("Please enter IP of directory server: ");
//        ip = "178.128.118.155";
//
//        // Get port of directory server
//        ps.println("Please enter port of directory server: ");
//        port = "7777";


        // Gets IP of directory server
        ps.println("Please enter IP of directory server: ");
        ip = sc.nextLine().trim(); // "178.128.118.155";

        // Get port of directory server
        ps.println("Please enter port of directory server: ");
        port = sc.nextLine(); // "7777";

        ps.println("Attempting to connect to directory server at " + ip + " (port "+ port + ")... ");
        return new Socket(ip,Integer.parseInt(port));
    }

    // Sends the client's public details to the sending peer through the relay server
    public static  void relayPeer(String pubIP, int pubPort, String targetIp, int targetPort){
        String reply;
        try {
            Socket socket = new Socket("178.128.118.155", 5001);
            PrintWriter requestWriter = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader ois = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            requestWriter.println(pubIP + "," + String.valueOf(pubPort) + "," + targetIp + "," + String.valueOf(targetPort));
            do {
                reply = ois.readLine();
            } while (!reply.equals("Ready"));
        } catch(IOException e) {
            System.out.println(e);
        }
    }
    
    // Prints reply from server
    public static void printServerReply(BufferedReader src) throws IOException {
        String data = null;
        while ((data = src.readLine()) != null) {
            // When end of packet has been reached
            if (data.equals("EOP")) {
                ps.print("\n");
                break;
            } else {
                ps.println(data);
            }
        }
    }

    // Receives FileInfo of file from directory server and adds it into local FileDirectory
    public static void readData(BufferedReader src, String fileName) throws IOException{
        String data = null;
        String [] args = null;
        String [] args1 = null;
        FileInfo tempFileInfo;
        
        while ((data = src.readLine()) != null) {
            // When end of packet has been reached
            if (data.equals("EOP"))
                break;

            args = data.split(DELIMITER);
            if (args[0].equals("D")) {
                args1 = args[1].split(",");
                // FileInfo(fileName, numChunks)
                fileDir.getFiles().add(
                    tempFileInfo = new FileInfo(
                        fileName,                   // File Name
                        Integer.parseInt(args1[0]), // Number of chunks
                        Integer.parseInt(args1[1])  // File size
                    )
                );

                for(int i = 0;i < Integer.parseInt(args1[0]);i++){
                    String [] args2 = args1[2 + i].split(" ");
                    tempFileInfo.addPeer(
                        i+1,                        // Chunk number
                        args2[0],                   // Public IP
                        Integer.parseInt(args2[1]), // Public Port
                        args2[2],                   // Private IP
                        Integer.parseInt(args2[3])  // Private Port
                    );
                }
            }
        }
    }

    // UDP hole puncher
    protected static void udpHolePunching(String targetIP, int targetPort, int srcPort) {
        try {
            byte[] sendData = "HolePunch".getBytes();
            DatagramSocket clientSocket = new DatagramSocket(srcPort);

            sendData = "Hello".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(targetIP), targetPort);
            clientSocket.send(sendPacket);
            clientSocket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Gets public IP and port (resolve NAT)
    private static CustomPeer getPublicIPnPort(String ip, int port) {
        try {
            String relayServerHostName = "178.128.118.155"; // STUN server's IP
            int relayServerPort = 5002; // STUN server port
            Socket clientSocket = new Socket();
            clientSocket.bind(new InetSocketAddress(ip,port)); // Relay port is the port we need to use for relaying
            clientSocket.connect(new InetSocketAddress("178.128.118.155",relayServerPort));

            PrintWriter requestWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader recvin = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String response = recvin.readLine();
            String[] splitResponse = response.split("-");
            publicIP = InetAddress.getByName(splitResponse[0].substring(0)).getHostAddress();
            publicPort = Integer.parseInt(splitResponse[1]);

            System.out.println("Private IP (port): " + ip + "(" + port + ").");
            System.out.println("Public IP (port):  " + publicIP + "(" + publicPort+ ").");
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new CustomPeer(publicIP,publicPort);
    }
}

// File directory class that contains a ArrayList of FileInfo
class FileDirectory {
    private List<FileInfo> files;

    public FileDirectory() {this.files = new ArrayList<FileInfo>();}

    public List<FileInfo> getFiles() {return this.files;}

    public FileInfo getFileInfoByFileName(String desiredFile) {
        FileInfo fileInfoToReturn = null;

        for(int i = 0;i < files.size();i++){
            if (files.get(i).getFilename().equals(desiredFile)){
                fileInfoToReturn = files.get(i);
            }
        }

        return fileInfoToReturn;
    }
}

// File info class that contains information of a file: file name, file size and HashMap of chunk information
class FileInfo {
    private String fileName;
    private int fileSize, numChunks;
    private boolean [] chunkAvailabilityMap;
    private HashMap<Integer,Peer> chunkInfo;

    public FileInfo (String name, int numChunks, int size) {
        this.fileName = name;
        this.fileSize = size;
        this.numChunks = numChunks;
        this.chunkAvailabilityMap = new boolean[numChunks];
        
        for(int i = 0;i < chunkAvailabilityMap.length;i++){
            chunkAvailabilityMap[i] = false;
        }
        
        this.chunkInfo = new HashMap<>();
    }

    public String getFilename() {return this.fileName;}

    public int getFileSize() {return this.fileSize;}

    public int getNumChunks() {return this.numChunks;}

    public boolean [] getChunkAvailabilityMap() {return this.chunkAvailabilityMap;}

    public Peer getChunkInfo(int key) {
        return this.chunkInfo.get(key);
    }

    public void addPeer(int chunkNum, String publicIP, int publicPort, String privateIP, int privatePort) {
        chunkInfo.put(chunkNum, new Peer(publicIP,publicPort,privateIP,privatePort));
    }
}

// Peer class that contains ip and port information of peer
class Peer {
    private String privateIP, publicIP;
    private int privatePort, publicPort;

    public Peer(String publicIP, int publicPort,String privateIP, int privatePort) {
        this.publicIP = publicIP;
        this.privateIP  = privateIP;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
    }

    public String getPublicIP() {return this.publicIP;}

    public String getPrivateIP() {return this.privateIP;}

    public int getPublicPort() {return this.publicPort;}

    public int getPrivatePort() {return  this.privatePort;}
}

// Custom Peer class
class CustomPeer {
    private String ip;
    private int port;

    public CustomPeer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIP() {return this.ip;}

    public int getPort() {return this.port;}

}