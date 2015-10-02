package as;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import numberTheory.AES;
import question.Question;

public class CommunicateToClient extends Thread {
	private AS server;
	private InputStream inputStream;
	private Socket socket;
	private String SSN;
	private BigInteger nounce1;
	private BigInteger nounce2;
	private int clientNumber;
	private BigInteger n;
	private BigInteger e;
	private BigInteger choice = BigInteger.ZERO;

	private BufferedReader bin;
	private PrintWriter pout;
	private SecretKey sessionKey;
	private boolean isVote = false;

	public SecretKey getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(SecretKey sessionKey) {
		this.sessionKey = sessionKey;
	}

	private int SESSION_KEY_LENGTH = 128;
	private int blockLength = 100;

	public CommunicateToClient(AS s, Socket socket, int clientNumber) {
		server = s;
		this.socket = socket;
		this.setClientNumber(clientNumber);
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
	 * @definition this method send data from server to client!
	 * @param data
	 */
	public void sendToClient(String data) {
		// System.err.println("send to client: " + data);
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

	public String decrypt(String message) {
		if (sessionKey == null) {
			return server.decrypt(message);
		} else {
			try {
				byte[] b = new BigInteger(message).toByteArray();
				return AES.decryptCBC(b, sessionKey);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	private boolean checkSignature(String message, String sign) {
		BigInteger s = null;
		try {
			s = new BigInteger(sign);

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		BigInteger hashPlain = s.modPow(e, n);
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

	private void sendQuestionToClient() {
		Question q = server.getQ();
		String m = "question " + q.getQuestion() + "#" + q.getNumberOfChoices()
				+ " ";
		int len = q.getNumberOfChoices();
		for (int i = 0; i < len - 1; i++) {
			m = m + q.getChoice(i).getChoice() + " ";
		}
		m += q.getChoice(len - 1).getChoice();
		String c = null;
		try {
			c = new BigInteger(AES.encryptCBC(m, sessionKey)) + "";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendToClient(c);
	}

	/**
	 * @definition this method do the right thing according to the received
	 *             message!!!
	 * @param command
	 * @throws IOException
	 */
	private void handleMessage(String command) throws Exception {
		server.getLog().addPacketToLog(command);
		String[] splitOfCommands = command.split("\\s+");
		String cmd = splitOfCommands[0];
		if (cmd.equals("hello")) {
			n = new BigInteger(splitOfCommands[1]);
			e = new BigInteger(splitOfCommands[2]);
			sendToClient("publicKey " + server.getN() + " " + server.getE()
					+ " " + server.getComToVs().getVsN() + " "
					+ server.getComToVs().getVsE());
			server.getLog().logClientInfo(e, n);
		} else {
			command = decrypt(command);
			splitOfCommands = command.split("\\s+");
			cmd = splitOfCommands[0];
			if (cmd.equals("start")) {
				String nounce = splitOfCommands[1];
				String SSN = splitOfCommands[2];
				String sign = splitOfCommands[3];
				if (checkSignature(nounce + " " + SSN, sign)) {
					server.getLog().logSSNandNounce1(SSN, nounce, sign, true);
					this.SSN = SSN;
					server.acceptClient(this);
					nounce1 = new BigInteger(nounce);
					KeyGenerator k = KeyGenerator.getInstance("AES");
					k.init(SESSION_KEY_LENGTH);
					sessionKey = k.generateKey();
					String sk = Base64.getEncoder().encodeToString(
							sessionKey.getEncoded());

					BigInteger s = server.makeSignature(nounce1 + " " + sk);
					String c = encrypt("sessionKey " + nounce1 + " " + sk + " "
							+ s + " ");
					sendToClient(c);
					sendQuestionToClient();
				} else {
					server.getLog().logSSNandNounce1(SSN, nounce, sign, false);
				}
			} else if (cmd.equals("sign")) {
				String nounce = splitOfCommands[1];
				String blind = splitOfCommands[2];
				if (checkSignature(nounce + " " + blind, splitOfCommands[3])) {
					server.getLog().logBlindMessage(nounce, blind, SSN,
							splitOfCommands[3], true);
					nounce2 = new BigInteger(nounce);
					String c = new BigInteger(AES.encryptCBC("signed "
							+ nounce2 + " " + getBlindSignature(blind),
							sessionKey))
							+ "";
					sendToClient(c);
				} else {
					server.getLog().logBlindMessage(nounce, blind, SSN,
							splitOfCommands[3], false);
				}
			} else if (cmd.equals("vote")) {
				if (isVote)
					return;
				String voteWithBlindSign = splitOfCommands[1];
				BigInteger n = new BigInteger(splitOfCommands[2]);
				server.getLog().logVote(SSN, voteWithBlindSign, n + "");
				if (n.equals(nounce2)) {
					int seq = server.getVote(this);
					String m = "vote " + voteWithBlindSign + " " + seq + " ";
					server.getComToVs().encryptAndSendToVs(m);
				}
			}
		}
		server.getLog().endOfPacket();
	}

	private String getBlindSignature(String blind) {
		BigInteger b = new BigInteger(blind);
		b = b.modPow(server.getD(), server.getN());
		return b + "";
	}

	public String encrypt(String message) {
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
				BigInteger bi = m.modPow(e, n);
				cipher = cipher + bi + "#";
			}
		}
		return cipher;
	}

	public int getClientNumber() {
		return clientNumber;
	}

	public void setClientNumber(int clientNumber) {
		this.clientNumber = clientNumber;
	}

	public String getSSN() {
		return SSN;
	}

	public void setSSN(String SSN) {
		this.SSN = SSN;
	}

	public BigInteger getN() {
		return n;
	}

	public BigInteger getE() {
		return e;
	}

	public BigInteger getChoice() {
		return choice;
	}

	public void setChoice(BigInteger choice) {
		this.choice = choice;
	}

	public boolean isVote() {
		return isVote;
	}

	public void setVote(boolean isVote) {
		this.isVote = isVote;
		if (isVote == true) {
			BigInteger sign = server.makeSignature(nounce2 + "");
			try {
				String c = new BigInteger(AES.encryptCBC("ACK " + nounce2 + " "
						+ sign, sessionKey))
						+ "";
				sendToClient(c);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}