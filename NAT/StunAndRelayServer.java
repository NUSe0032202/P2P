import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class StunAndRelayServer {


	public StunAndRelayServer() { }

	public static void main(String[] args) {
		StunAndRelayServer stunRelayServer = new StunAndRelayServer();

		stunRelayServer.run();
	}

	private void run() {
		try {
			StunRunnable stunRunnable = new StunRunnable();
			Thread stun = new Thread(stunRunnable);
			stun.start();


			RelayRunnable relayRunnable = new RelayRunnable();
			Thread relay = new Thread(relayRunnable);
			relay.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}



class RelayRunnable implements Runnable{
	static int port = 5001;
	static HashMap<String,Socket> peerToSocket = new HashMap<>();
	@Override
		public void run() {
			try {
				ServerSocket serverSocket = new ServerSocket(port);//Creates a new socket on the server side
				System.out.println("Relay Server listening on port " + port);
				while (true) {//Loops forever
					new RelayThread(serverSocket.accept(),peerToSocket).start();//Accept a new connection and start a thread
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
}


class RelayThread extends Thread {

	private Socket socket;
	private static HashMap<String,Socket> mapping;


	public RelayThread(Socket socket,HashMap <String,Socket> peerToSocket) {
		this.socket = socket;
		this.mapping = peerToSocket;

		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  //
			int keyPort = Integer.valueOf(in.readLine());                                            //
			mapping.put(socket.getInetAddress().getHostAddress()+"-"+keyPort,socket);
                        System.out.println("before hash ip:" + socket.getInetAddress().getHostAddress() + ",before hash port:" + keyPort);    //
			if(socket==null){
				System.out.println("socket is null");
			}else{
				System.out.println("socket is not null");
			}
			//in.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public void run() {

		while(true){
			try {

			
			//PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			//String IPAddress1 = socket.getInetAddress().getHostAddress();
			//int port1 = socket.getPort();

			//System.out.println("MsgForClient: " + msgForClient);


			//***********************************************************************************
			while(in.readLine() == null){};//Keep waiting until receive something
			String cmd = in.readLine();
			System.out.println(cmd);
			String [] args = cmd.split(",");
			Socket connect = mapping.get(args[2]+"-"+Integer.parseInt(args[3])); //get
			if(connect ==null){
				System.out.println("connect is null");
			}else{
				System.out.println("connect is not null");
			}

			System.out.println("size of map = "+mapping.size());
			
                        System.out.println("args2:" + args[2] + ",args3:" + args[3]);
			PrintWriter out_sender = new PrintWriter(connect.getOutputStream(), true);
			out_sender.println("Request received !");
			out_sender.println(args[0] + "," + args[1] + "," + args[4] + "," + args[5]);
			/////////////////////////////////////////////////////////////////////////////////////////
			


			//String[] args = cmd.split(",");
			//System.out.println(cmd);
			//String targetIP = splitDest[0].substring(0);
			//int targetPort =  Integer.parseInt(splitDest[1]);
			//Socket targetSocket = new Socket(args[2],Integer.parseInt(args[3]));
			//PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
			//BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));
			//String msgForClient = "HP" + "," + args[0] + "," + args[1];
			//targetOut.println(msgForClient);
			//String toSend = targetIn.readLine();
			//out.println(toSend);

		} catch (Exception e) {
			e.printStackTrace();
		}
		}
	}
}
class CustomPeer {

	private String IP;
	private int port;

	public CustomPeer (String ip , int Port) {
		this.IP = ip;
		this.port = Port;
	}

	@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CustomPeer cmp = (CustomPeer) o;
			return Objects.equals(IP, cmp.IP) &&
				Objects.equals(port,cmp.port);
		}

}
class StunRunnable implements Runnable{
	static int port = 5002;


	@Override
		public void run() {
			try {
				ServerSocket serverSocket = new ServerSocket(port);//Creates a new socket on the server side
				System.out.println("Stun Server listening on port " + port);
				while (true) {//Loops forever
					new StunThread(serverSocket.accept()).start();//Accept a new connection and start a thread
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
}



class StunThread extends Thread {

	private Socket socket;

	public StunThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		try {

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String IPAddress1 = socket.getInetAddress().getHostAddress();
			int port1 = socket.getPort();
			String msgForClient = IPAddress1 + "-" + port1;
			System.out.println("MsgForClient: " + msgForClient);

			out.println(msgForClient);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
