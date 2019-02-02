import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;

public class DirectoryServerListen {
    // Each chunk is mapped to a list of all peers having that chunk
    private static HashMap<Chunk, ArrayList<Peer>> chunkToPeerMapping = new HashMap<>();
    // Each peer is mapped to a list of all chunks that peer has
    private static HashMap<Peer, ArrayList<Chunk>> availChunksAtPeer = new HashMap<>();
    // Each filename is mapped to a FileEntry containing all info about the file
    private static HashMap<String, FileEntry> filenameToFileEntry = new HashMap<>();

    private int connectedP;

    public DirectoryServerListen() {
        this.connectedP = 0;
    }

    public static void main(String[] args) {
        DirectoryServerListen dirServer = new DirectoryServerListen();
        //Insert test method here to pre-load directory server with values
        test(dirServer);
        dirServer.run();
    }

    private void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(7777);//Creates a new socket on the server side
            while (true) {//Loops forever
                new DirectoryServerThread(serverSocket.accept(), (connectedP += 1), this).start();//Accept a new connection and start a thread
                System.out.println(connectedP + " client(s) are connected !");
            }
        } catch (IOException exception) {
            System.out.println("Error when creating the server socket");
        }
    }
    public static void test (DirectoryServerListen dirServer){
        ArrayList<Chunk> toAdd = new ArrayList<>();
        toAdd.add(new Chunk(new FileEntry("vid1.3gpp",300000,50000),1)); //Chunk one
        toAdd.add(new Chunk(new FileEntry("vid1.3gpp",300000,50000),2));
        toAdd.add(new Chunk(new FileEntry("vid1.3gpp",300000,50000),3));
        toAdd.add(new Chunk(new FileEntry("vid1.3gpp",300000,50000),4));
        toAdd.add(new Chunk(new FileEntry("vid1.3gpp",300000,50000),5));
        toAdd.add(new Chunk(new FileEntry("vid1.3gpp",300000,50000),6)); 
        //toAdd.add(new Chunk(new FileEntry("vid1.3gpp",400000,200000),2)); //Chunk two
        dirServer.update(new Peer("host@127.0.0.1","137.132.181.1",4000,"172.25.102.118",3001),toAdd);
        //File details has to be that of the peer that has that particular file
    }
    /**
     * This method takes in a peer and available chunks at that peer,
     * adds new chunks (if any) to directory server
     *
     * @param peer
     * @param availChunks
     */
    //public static HashMap<Chunk, ArrayList<Peer>> (){}
    synchronized protected static void update(Peer peer, ArrayList<Chunk> availChunks) {
        for (Chunk chunk : availChunks) {
            ArrayList<Peer> curPeers;
            if (chunkToPeerMapping.containsKey(chunk)) {
                curPeers = chunkToPeerMapping.get(chunk);
            } else {
                curPeers = new ArrayList<>();
            }
            curPeers.add(peer);
            chunkToPeerMapping.put(chunk, curPeers);//Old value for the hashmap is replaced
        }

        availChunksAtPeer.put(peer, availChunks);

        for (Chunk chunk : availChunks) {
            FileEntry fileInfo = chunk.getFile();
            filenameToFileEntry.put(fileInfo.getFilename(), fileInfo);
        }
    }

    /**
     * This method returns list of all files in directory server
     *
     * @return ArrayList<FileEntry>
     */
    protected static ArrayList<FileEntry> listAllFiles() {
        return new ArrayList<>(filenameToFileEntry.values());
    }

    /**
     * This method returns list of all peers having a particular chunk
     *
     * @param requestedChunk
     * @return ArrayList<Peer>
     */
    protected static ArrayList<Peer> getChunk(Chunk requestedChunk) {
        if (chunkToPeerMapping.containsKey(requestedChunk)) {
            return chunkToPeerMapping.get(requestedChunk);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * This method removes a peer and all chunks it has from directory server
     *
     * @param peer
     */
    synchronized protected static void removePeer(Peer peer) {
        // get all chunks associated with the peer to remove
        ArrayList<Chunk> chunksToRemove = availChunksAtPeer.get(peer);

        for (Chunk chunk : chunksToRemove) {
            chunkToPeerMapping.get(chunk).remove(peer);
            if (chunkToPeerMapping.get(chunk).isEmpty()) {
                chunkToPeerMapping.remove(chunk);
            }

           if (!isFileAvailable(chunk.getFile())) {
                filenameToFileEntry.remove(chunk.getFile().getFilename());
           }
        }

        // remove all files/chunks associated with the peer
        availChunksAtPeer.remove(peer);
    }

    /**
     * This method checks whether a file is still available in the directory
     * i.e, at least a chunk is still available at peer(s)
     *
     * @param file
     * @return TRUE if the file is still available, FALSE otherwise
     */
    private static boolean isFileAvailable(FileEntry file) {
        for (int chunkIndex = 0; chunkIndex < file.getNumChunks(); chunkIndex++) {
            if (chunkToPeerMapping.containsKey(new Chunk(file, chunkIndex))) {
                return true;
            }
        }
        return false;
    }

    public static HashMap<String, FileEntry> getFilenameToFileEntry() {
        return filenameToFileEntry;
    }
}
