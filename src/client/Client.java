package client;

import numberTheory.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.SecretKey;

import numberTheory.RSA;
import question.Question;

public class Client extends Thread {
	private CommunicationToServer communicationToServer;
	private Socket socket;
	private String host = "127.0.0.1";
	private int port = 1717;
	private String SSN = "";
	private Question question;
	private Scanner sc;
	private BigInteger n;
	private BigInteger e;
	private BigInteger d;
	private boolean isVote = false;

	private int blockLength = 100;

	private BigInteger serverN;
	private BigInteger serverE;

	private BigInteger vsE;
	private BigInteger vsN;
	private AbstractClient[] clients;
	private SecretKey sessionKey = null;

	public Client() {
		RSA r = new RSA(1024);
		n = r.getN();
		e = r.getE();
		d = r.getD();
	}

	@Override
	public void run() {
		sc = new Scanner(System.in);
		try {
			while (true) {
				if (SSN.length() != 0) {
					while (true) {
						String[] login = sc.nextLine().split(" ");
						if (login.length == 2 && login[0].equals("Login")
								&& login[1].equals(SSN))
							break;
					}
				}
				while (true) {
					String command = sc.nextLine();
					if (!handleCommand(command))
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.run();
	}

	public String encrypt(String message, String destination) {
		if (sessionKey == null || destination.equals("VS")) {
			return encryptWithPrivateKey(message, destination);
		} else
			try {
				return new BigInteger(AES.encryptCBC(message, sessionKey)) + "";
			} catch (Exception e) {
				e.printStackTrace();
			}
		return null;
	}

	private String encryptWithPrivateKey(String message, String destination) {
		byte[] bytes = message.getBytes();
		int len = bytes.length;
		int numOfBlocks = len / blockLength + 1;
		String cipher = "";
		BigInteger destE = serverE;
		BigInteger destN = serverN;
		if (destination.equals("VS")) {
			destE = vsE;
			destN = vsN;
		}
		try {
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
					BigInteger bi = m.modPow(destE, destN);
					cipher = cipher + bi + "#";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cipher;
	}

	public BigInteger makeSignature(String message) {
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			BigInteger hash = new BigInteger(m.digest(message.getBytes()))
					.mod(n);
			return hash.modPow(d, n);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean checkSignature(String message, String sign) {
		BigInteger s = new BigInteger(sign);
		BigInteger hashPlain = s.modPow(serverE, serverN);
		try {
			MessageDigest hash = MessageDigest.getInstance("MD5");
			if (new BigInteger(hash.digest(message.getBytes())).mod(n).equals(
					hashPlain))
				return true;
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	public String decrypt(String message) {
		if (sessionKey == null) {
			return decryptWithPrivateKey(message);
		} else
			try {
				byte[] b = new BigInteger(message).toByteArray();
				return AES.decryptCBC(b, sessionKey);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return null;
	}

	public SecretKey getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(SecretKey sessionKey) {
		this.sessionKey = sessionKey;
	}

	private String decryptWithPrivateKey(String message) {
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
	 * @definition this method handle command that client enter it!
	 * @param command
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private boolean handleCommand(String command) throws UnknownHostException,
			IOException {
		if (command.equals("Login")) {
			socket = new Socket(host, port);
			setSSN(getSSN());
			communicationToServer = new CommunicationToServer(socket, this);
			communicationToServer.start();
			communicationToServer.sendToServer("hello " + n + " " + e);
		} else if (command.equals("Question")) {
			question.printTheQuestion();
		} else if (command.equals("Vote")) {
			if (isVote) {
				System.out.println("You voted before, your choice is :");
				question.getChoices()[Integer.parseInt(communicationToServer
						.getChoice() + "")].printChoice();
			} else {
				getVoteAndSendToServer();
			}
		} else if (command.equals("Exit")) {
			return false;
		}
		return true;
	}

	public void createAbstractClients(int numOfClients) {
		clients = new AbstractClient[numOfClients];
	}

	private BigInteger makeBlindMessage(BigInteger message) {
		BigInteger r = findRelativePrime();
		BigInteger r2 = r.modPow(getServerE(), getServerN());
		communicationToServer.setR(r);
		return message.multiply(r2).mod(getServerN());
	}

	private BigInteger findRelativePrime() {
		while (true) {
			BigInteger n = getServerN();
			int len = n.bitLength();
			BigInteger r = new BigInteger(len, new Random());
			if (r.gcd(n).equals(new BigInteger("1")))
				return r;
		}
	}

	/**
	 * @definition this method get vote from client and send them to server!
	 */
	private void getVoteAndSendToServer() {
		String ch = sc.nextLine();
		BigInteger choice = new BigInteger(ch);
		choice = choice.subtract(new BigInteger("1"));
		communicationToServer.setChoice(choice);
		BigInteger blind = makeBlindMessage(choice);
		BigInteger nounce2 = communicationToServer.makeNounce();
		communicationToServer.setNounce2(nounce2);
		BigInteger sign = makeSignature(nounce2 + " " + blind);
		String c = encrypt("sign " + nounce2 + " " + blind + " " + sign + " ",
				"AS");
		communicationToServer.sendToServer(c);
	}

	public BigInteger getHOfClient(int clientNumber) {
		return clients[clientNumber].getH();
	}

	private String getSSN() {
		System.out.println("Please enter your SSN :");
		return sc.nextLine();
	}

	public void setSSN(String SSN) {
		this.SSN = SSN;
	}

	public String getClientSSN() {
		return SSN;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(String q, String[] choices) {
		int numOfChoices = choices.length;
		question = new Question(q, numOfChoices, 1);
		for (int i = 0; i < numOfChoices; i++) {
			question.addChoice(choices[i], i);
		}
	}

	public BigInteger getServerN() {
		return serverN;
	}

	public void setServerN(BigInteger serverN) {
		this.serverN = serverN;
	}

	public BigInteger getServerE() {
		return serverE;
	}

	public void setServerE(BigInteger serverE) {
		this.serverE = serverE;
	}

	public void setVsE(BigInteger vsE) {
		this.vsE = vsE;
	}

	public void setVsN(BigInteger vsN) {
		this.vsN = vsN;
	}
	
	public void setQustion(Question q) {
		this.question = q;
	}

	public void setVote(boolean isVote) {
		this.isVote = isVote;
	}
}
