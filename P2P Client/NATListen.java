import java.util.*;
import java.net.*;
import java.io.*;

public class NATListen extends Thread {
    private Socket socket;
    private int port;
    public static final int CHUNK_SIZE = 50000;

    public NATListen (Socket src,int srcPort){
        super("NAT listener thread");
        this.socket = src;
        this.port = srcPort;
    }
    public void run (){
        while(true) {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                while (input.readLine() == null) {
                }
                String recv = input.readLine();
                System.out.println(recv);
                String[] args = recv.split(",");
                DatagramSocket clientSocket = new DatagramSocket(port);//This port here has to be
                int chunkNo = Integer.parseInt(args[1]);
                RandomAccessFile fileReader = new RandomAccessFile(args[0], "r");
                int length = 0;
                int start = 0;
                byte[] buffer = new byte[CHUNK_SIZE];
                if (chunkNo == 1) {
                    start = 0;
                } else {
                    start = ((chunkNo - 1) * CHUNK_SIZE) - 1;
                }
                fileReader.seek(start);
                System.out.println("Starting position is at: " + start);
                length = fileReader.read(buffer, 0, CHUNK_SIZE);
                System.out.println("Bytes read from file: " + length);
                DatagramPacket sendPacket = new DatagramPacket(buffer, length, InetAddress.getByName(args[2]), Integer.parseInt(args[3]));
                clientSocket.send(sendPacket);
                clientSocket.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}