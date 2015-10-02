package as;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;

import question.Question;

public class CommunicationToVS extends Thread{
	private Socket s;
	private String host = "127.0.0.1";
	private int port = 4030;
	private AS as;
	private BigInteger vsE;
	private BigInteger vsN;
	private int blockLength = 100;
	
	
	private InputStream inputStream;
	private BufferedReader bin;
	private PrintWriter pout;
	
	@Override
	public void run() {
		super.run();
		sendToVS("hello " + as.getN() + " " + as.getE());
		try {// server listening to client!
			while (true) {
				String command = bin.readLine();
				handleMessage(command);
			}
		} catch (Exception e) {
			System.exit(0);
		}
	}
	
	public void sendQuestionToVS(Question q){
		String m = "question " + q.getQuestion() + "#" + q.getNumberOfChoices() + " ";
		int len = q.getNumberOfChoices();
		for (int i = 0; i < len - 1; i++) {
			m = m + q.getChoice(i).getChoice() + " ";
		}
		m += q.getChoice(len - 1).getChoice();
		encryptAndSendToVs(m + " ");
	}
	
	private void handleMessage(String command) {
		String[] splitOfCommands = command.split(" ");
		String cmd = splitOfCommands[0];
		if(cmd.equals("hello")){
			setVsN(new BigInteger(splitOfCommands[1]));
			setVsE(new BigInteger(splitOfCommands[2]));
			as.getLog().logVsInfo(vsE, vsN);
		}
		else {
			command = decrypt(command);
			splitOfCommands = command.split("\\s+");
			cmd = splitOfCommands[0];
			if(cmd.equals("ACK")){
				int seq = 0;
				BigInteger i = new BigInteger(splitOfCommands[1]);
				seq = Integer.parseInt(i + "");
				as.saveVote(seq);
				as.getLog().logAckMessage(seq + "");
			}
		}
		as.getLog().endOfPacket();
	}
	
	private String decrypt(String message){
		String[] blocks = message.split("#");
		int len = blocks.length;
		String plain = "";
		for (int i = 0; i < len; i++) {
			BigInteger b = new BigInteger(blocks[i]);
			BigInteger c = b.modPow(as.getD(), as.getN());
			String s = new String(c.toByteArray());
			plain += s;
		}
		return plain;
	}
	
	public String encryptAndSendToVs(String message) {
		byte[] bytes = message.getBytes();
		int len = bytes.length;
		int numOfBlocks = len / blockLength + 1;
		String cipher = "";
		for (int i = 0; i < numOfBlocks; i++) {
			byte[] block = new byte[blockLength];
			int offset = i * blockLength;
			int max = blockLength;
			if (i == numOfBlocks - 1) {
				max = len - i * blockLength;
			}
			for (int j = 0; j < max; j++) {
				block[j] = bytes[offset + j];
			}
			if (max != 0) {
				BigInteger m = new BigInteger(block);
				BigInteger bi = m.modPow(vsE, vsN);
				cipher = cipher + bi + "#";
			}
		}
		sendToVS(cipher);
		return cipher;
	}

	/**
	 * @definition this method initialize the input and output stream!
	 */
	private void initializeCommunication() {
		try {
			inputStream = s.getInputStream();
			bin = new BufferedReader(new InputStreamReader(inputStream));
			pout = new PrintWriter(s.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @definition this method send data from client to server!
	 * @param data
	 */
	public void sendToVS(String data) {
		pout.println(data);
		pout.flush();
	}
	
	public CommunicationToVS(AS as) {
		try {
			this.as = as;
			s = new Socket(host, port);
			initializeCommunication();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BigInteger getVsE() {
		return vsE;
	}

	public void setVsE(BigInteger vsE) {
		this.vsE = vsE;
	}

	public BigInteger getVsN() {
		return vsN;
	}

	public void setVsN(BigInteger vsN) {
		this.vsN = vsN;
	}
}
