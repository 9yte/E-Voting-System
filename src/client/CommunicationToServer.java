package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import question.Question;


public class CommunicationToServer extends Thread {
	private Socket socket;
	private Client client;
	private InputStream inputStream;
	private BufferedReader bin;
	private PrintWriter pout;
	BigInteger one = new BigInteger("1");
	BigInteger two = new BigInteger("2");
	
	private BigInteger nounce1;
	private BigInteger nounce2;

	private BigInteger r;

	private BigInteger choice;
	private int NOUNCE_LENGTH = 100;

	public CommunicationToServer(Socket socket, Client client) {
		this.socket = socket;
		this.client = client;
		initializeCommunication();
	}

	/**
	 * @definition this method initialize the input and output stream!
	 */
	private void initializeCommunication() {
		try {
			inputStream = socket.getInputStream();
			bin = new BufferedReader(new InputStreamReader(inputStream));
			pout = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @definition this method send data from client to server!
	 * @param data
	 */
	public void sendToServer(String data) {
//		System.err.println("send to server: " + data);
		pout.println(data);
		pout.flush();
	}

	@Override
	public void run() {
		super.run();
		try {// server listening to client!
			while (true) {
				String command = bin.readLine();
				handleMessage(command);
			}
		} catch (Exception e) {
			System.exit(0);
		}
	}

	/**
	 * @definition this method do the right thing after receiving command from client
	 * @param command
	 * @throws Exception
	 */
	private void handleMessage(String command) throws Exception {
		String[] splitOfCommands = command.split(" ");
		String cmd = splitOfCommands[0];
		if (cmd.equals("publicKey")){
			client.setServerN(new BigInteger(splitOfCommands[1]));
			client.setServerE(new BigInteger(splitOfCommands[2]));
			client.setVsN(new BigInteger(splitOfCommands[3]));
			client.setVsE(new BigInteger(splitOfCommands[4]));
			nounce1 = makeNounce();
			BigInteger s = client.makeSignature(nounce1 + " " + client.getClientSSN());
			String message = "start " + nounce1 + " " + client.getClientSSN() + " " + s + " ";
			String cipher = client.encrypt(message, "AS");
			sendToServer(cipher);
		}
		else{
			command = client.decrypt(command);
			splitOfCommands = command.split(" ");
			cmd = splitOfCommands[0];
			if (cmd.equals("sessionKey")){
				BigInteger n = new BigInteger(splitOfCommands[1]);
				String sessionKey = splitOfCommands[2];
				if(client.checkSignature(n + " " + sessionKey, splitOfCommands[3])){
					if (n.equals(nounce1)){
						byte[] decodedKey = Base64.getDecoder().decode(sessionKey);
						SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
						client.setSessionKey(originalKey);
					}					
				}
			}
			else if(cmd.equals("signed")){
				BigInteger n = new BigInteger(splitOfCommands[1]);
				if(nounce2.equals(n)){
					BigInteger s = new BigInteger(splitOfCommands[2]);
					BigInteger inv = r.modInverse(client.getServerN());
					s = s.multiply(inv).mod(client.getServerN());
					String m = choice + " " + s + " ";
					String c = client.encrypt(m, "VS");
					c = "vote " + c + " " + nounce2 + " ";
					c = client.encrypt(c, "AS");
					sendToServer(c);
				}
			}
			else if(cmd.equals("question")){
				String[] qq = command.split("#");
				String[] qqqq = qq[1].split(" ");
				String q = qq[0].substring(8);
				int numOfChoices = Integer.parseInt(qqqq[0]);
				Question question = new Question(q, numOfChoices, 1);
				for (int i = 0; i < numOfChoices; i++) {
					String c = qqqq[1 + i];
					question.addChoice(c, i);
				}
				client.setQustion(question);
			}
			else if(cmd.equals("ACK")){
				BigInteger n = new BigInteger(splitOfCommands[1]);
				String sign = splitOfCommands[2];
				if(n.equals(nounce2)){
					if(client.checkSignature(n + "", sign)){
						client.setVote(true);
					}					
				}
			}
		}
	}
	
	public BigInteger makeNounce() {
		return new BigInteger(NOUNCE_LENGTH, new Random(System.currentTimeMillis()));
	}
	
	public BigInteger getR() {
		return r;
	}

	public void setR(BigInteger r) {
		this.r = r;
	}
	
	public BigInteger getChoice() {
		return choice;
	}

	public void setChoice(BigInteger choice) {
		this.choice = choice;
	}
	
	public BigInteger getNounce2() {
		return nounce2;
	}

	public void setNounce2(BigInteger nounce2) {
		this.nounce2 = nounce2;
	}
}
