package as;

import java.net.ServerSocket;
import java.net.Socket;

public class AcceptClient extends Thread {
	private ServerSocket sc;
	private int socketPort = 1717;
	AS server;

	public AcceptClient(AS server) {
		this.server = server;
	}

	@Override
	public void run() {
		super.run();
		try {
			sc = new ServerSocket(socketPort);
			while (true) {
					Socket clientSocket = sc.accept();
					System.out.println("Client accepted");
					CommunicateToClient client = new CommunicateToClient(server,
							clientSocket, server.getNumberOfClients());
					client.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
