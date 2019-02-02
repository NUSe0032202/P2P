import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.*;

//This class is responsible for responding to data requests from other peers
public class Listener extends Thread{
    public static final int CHUNK_SIZE = 400000;
    private int PORT;

    public Listener (int allocatedPort) {
        super("Client listener thread");
        PORT = allocatedPort;
        System.out.println("Starting client listener on: " + PORT);
    }
    public void run () {
        try {
            ServerSocket sSocket = new ServerSocket(PORT);//2nd port to listen on
            while (true) {
                Socket transfer = sSocket.accept();
                PrintWriter out = new PrintWriter(transfer.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(transfer.getInputStream()));
                String cmd = in.readLine();
                System.out.println(cmd);//Print received details
                String[] request = cmd.split(",");
              //  if(request[0].equals("HP")){
                //    System.out.println("Hole punch needed");
                 //   udpHolePunching(request[1],Integer.valueOf(request[2]),PORT);//Hole punch to receiving peer
                 //   out.println("Ready");
                  //  transfer.close();
               // }else {
                    int chunkNo = Integer.parseInt(request[1]);
                    RandomAccessFile fileReader = new RandomAccessFile(request[0], "r");
                    DataOutputStream clientWriter = new DataOutputStream(new BufferedOutputStream(transfer.getOutputStream()));
                    int length = 0;
                    int start = 0;
                    byte[] buffer = new byte[CHUNK_SIZE];
                    if (chunkNo == 1) {
                        start = 0;   //Set starting point
                    } else {
                        start = ((chunkNo - 1) * CHUNK_SIZE) - 1; //Set starting point..Chunk one start from zero
                    }
                    fileReader.seek(start); //Sets the starting position
                    System.out.println("Starting position is at: " + start);
                    length = fileReader.read(buffer, 0, CHUNK_SIZE);
                    System.out.println("Bytes read from file: " + length);
                    clientWriter.write(buffer, 0, length);
                    clientWriter.flush();
                    transfer.close();
                //}
            }
        }catch (IOException exception) {
            System.out.println(exception);
        }
    }
    protected static void udpHolePunching(String targetIP, int targetPort, int srcPort) {
        try {

            DatagramSocket clientSocket = new DatagramSocket();

            byte[] sendData = "HolePunch".getBytes();

            //int localPort = clientSocket.getLocalPort();
            clientSocket = new DatagramSocket(srcPort);

            clientSocket.setSoTimeout(1000);

            sendData = "Hello".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(targetIP), targetPort);
            clientSocket.send(sendPacket);
            clientSocket.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
