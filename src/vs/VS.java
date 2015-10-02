package vs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import javax.crypto.SecretKey;

import question.Question;
import numberTheory.MasterKeyGenerator;
import numberTheory.RSA;

public class VS {
	private ServerSocket sc;
	private Socket asSocket;
	private int port = 4030;

	private InputStream inputStream;
	private BufferedReader bin;
	private PrintWriter pout;
	private BigInteger e;
	private BigInteger n;
	private BigInteger d;
	private Question question;
	private BigInteger asE;
	private BigInteger asN;
	private int blockLength = 100;
	private Log log;

	private ArrayList<BigInteger>[] votes;
	private int[] numOfVotes;
	private SecretKey masterKey;

	public VS() {
		try {
			sc = new ServerSocket(port);
			masterKey = MasterKeyGenerator.generate();
			
			log = new Log();
			log.setMasterKey(masterKey);
			log.logCurrentDateTime();
			RSA r = new RSA(1024);
			e = r.getE();
			d = r.getD();
			n = r.getN();
			log.logInformation(e, d, n);
			new VSManager(this).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new VS().run();
	}

	public void run() {
		try {
			asSocket = sc.accept();
			System.out.println("AS connected");
			initializeCommunication();
			while (true) {
				String command = bin.readLine();
				handleMessage(command);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleMessage(String command) {
		log.addPacketToLog(command);
		String[] splitOfCommands = command.split(" ");
		String cmd = splitOfCommands[0];
		if (cmd.equals("hello")) {
			asN = new BigInteger(splitOfCommands[1]);
			asE = new BigInteger(splitOfCommands[2]);
			log.logAsInfo(asE, asN);
			String m = "hello " + n + " " + e;
			sendToAS(m);
		} else {
			command = decrypt(command);
//			log.logDecPacket(command);
			splitOfCommands = command.split(" ");
			cmd = splitOfCommands[0];
			if (cmd.equals("vote")) {
				String voteWithBlindSign = splitOfCommands[1];
				String[] vote = decrypt(voteWithBlindSign).split(" ");
				if (checkBlindSign(vote[0], vote[1])) {
					log.logVote(vote[0], vote[1]);
					int choiceNumber = Integer.parseInt(vote[0]);
					votes[choiceNumber].add(new BigInteger(vote[1]));
					numOfVotes[choiceNumber]++;
				}
				String seq = splitOfCommands[2];

				encryptAndSendToAs("ACK " + seq + " ");
			} else if (cmd.equals("question")) {
				String[] qq = command.split("#");
				String[] qqqq = qq[1].split(" ");
				String q = qq[0].substring(8);
				int numOfChoices = Integer.parseInt(qqqq[0]);
				question = new Question(q, numOfChoices, 1);
				for (int i = 0; i < numOfChoices; i++) {
					String c = qqqq[1 + i];
					question.addChoice(c, i);
				}
				votes = (ArrayList<BigInteger>[]) new ArrayList[numOfChoices];
				numOfVotes = new int[numOfChoices];
				for (int i = 0; i < numOfChoices; i++) {
					votes[i] = new ArrayList<BigInteger>();
				}
				log.logQuestion(question);
			}
		}
		log.endOfPacket();
	}

	private String encryptAndSendToAs(String message) {
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
				BigInteger bi = m.modPow(asE, asN);
				cipher = cipher + bi + "#";
			}
		}
		sendToAS(cipher);
		return cipher;
	}

	public void printResult() {
		System.out.println(question.getQuestion());
		int len = question.getNumberOfChoices();
		for (int i = 0; i < len; i++) {
			question.getChoice(i).printChoice();
			System.out.println(" -----> " + numOfVotes[i]);
		}
	}

	private boolean checkBlindSign(String vote, String sign) {
		BigInteger v = new BigInteger(vote);
		BigInteger s = new BigInteger(sign);
		BigInteger x = s.modPow(asE, asN);
		return x.equals(v);
	}

	public String decrypt(String message) {
		String[] blocks = message.split("#");
		int len = blocks.length;
		String plain = "";
		for (int i = 0; i < len; i++) {
			BigInteger b = new BigInteger(blocks[i]);
			BigInteger c = b.modPow(d, n);
			String s = new String(c.toByteArray());
			plain += s;
		}
		return plain;
	}

	/**
	 * @definition this method send data from client to server!
	 * @param data
	 */
	public void sendToAS(String data) {
		pout.println(data);
		pout.flush();
	}

	/**
	 * @definition this method initialize the input and output stream!
	 */
	private void initializeCommunication() {
		try {
			inputStream = asSocket.getInputStream();
			bin = new BufferedReader(new InputStreamReader(inputStream));
			pout = new PrintWriter(asSocket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Log getLog() {
		return log;
	}

	public void setLog(Log log) {
		this.log = log;
	}
}

class VSManager extends Thread {
	private VS vs;

	public VSManager(VS vs) {
		this.vs = vs;
	}

	@Override
	public void run() {
		super.run();
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		while (true) {
			String command = sc.nextLine();
			if (command.equals("result")) {
				vs.printResult();
			}
			else if(command.equals("decrypt")){
				vs.getLog().decryptLog();
			}
		}
	}
}
