package as;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Scanner;

import javax.crypto.SecretKey;

import question.*;
import numberTheory.*;

public class AS {
	private HashMap<String, CommunicateToClient> clients;
	private HashMap<Integer, CommunicateToClient> voteToClients;
	private int numberOfQuestions;
	private int numberOfClients = 0;
	private Scanner sc;
	private AcceptClient acceptClient;
	private BigInteger n;
	private BigInteger e;
	private BigInteger d;
	private CommunicationToVS comToVs;
	private int seqNumber = 0;
	private Question q;
	private Log log;
	private SecretKey masterKey;

	public AS() {
		clients = new HashMap<String, CommunicateToClient>();
		voteToClients = new HashMap<Integer, CommunicateToClient>();
		masterKey = MasterKeyGenerator.generate();
		log = new Log();
		log.setMasterKey(masterKey);
	}

	public void runServer() {
		try {
			RSA r = new RSA(1024);
			n = r.getN();
			e = r.getE();
			d = r.getD();
			sc = new Scanner(System.in);
			comToVs = new CommunicationToVS(this);
			comToVs.start();
			log.logInformation(e, d, n);
			while(true){
				acceptClient = new AcceptClient(this);
				acceptClient.start();
				while (true) {
					String command = sc.nextLine();
					if (!handleCommand(command))
						break;
				}
			}
		} catch (Exception e) {
		}
	}
	
	public int getVote(CommunicateToClient cts){
		voteToClients.put(seqNumber, cts);
		return seqNumber++;
	}

	public BigInteger makeSignature(String message) {
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			BigInteger hash = new BigInteger(m.digest(message.getBytes())).mod(n);
			return hash.modPow(d, n);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @definition this method do the right thing according to the given command
	 *             from console!
	 * @param command
	 * @return
	 * @throws IOException 
	 */
	private boolean handleCommand(String command) throws IOException {
		if (command.equals("Add")){
			getQuestion();
			log.logQuestion(q);
			log.endOfPacket();
			comToVs.sendQuestionToVS(q);
		}
		else if(command.equals("Check")){
			System.out.println("Print user SSN please!");
			String ssn = sc.nextLine();
			if (clients.containsKey(ssn)){
				CommunicateToClient c = clients.get(ssn);
				if(c.isVote()){
					System.out.println("Yes, this person has voted");
				}
				else
					System.out.println("Not yet!");
			}
		}
		else if(command.equals("decrypt")){
			log.decryptLog();
		}
		else if (command.equals("Exit")) {
			System.out.println("Good Bye!");
			return false;
		}
//		else if (command.equals("Result"))
//			calculateResult();
		return true;
	}
	
	public String decrypt(String message){
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
	 * @definition this method read question, and add it to the all questions!
	 */
	private void getQuestion() {
		// getting the question!
		System.out.println("Please enter the Question :");
		String question = "";
		while (true) {
			String token = sc.next();
			if (token.charAt(token.length() - 1) == '#') {
				if (token.length() > 1)
					question = question + " "
							+ token.substring(0, token.length() - 1);
				break;
			}
			question = question + " " + token;
		}
		question = question.substring(1);

		// getting number of choices!
		System.out.println("Please enter the number of choices :");
		int numOfSolutions = sc.nextInt();
		numberOfQuestions++;
		q = new Question(question, numOfSolutions, numberOfQuestions);

		// getting the choices!
		for (int i = 0; i < numOfSolutions; i++) {
			System.out.println("Please enter choice number " + (i + 1) + ":");
			q.addChoice(sc.next(), i);
		}
		System.out.println("The Question was added.");
	}

	/**
	 * @definition this method add newClient to clients of server
	 * @param newClient
	 */
	void acceptClient(CommunicateToClient newClient) {
		clients.put(newClient.getSSN(), newClient);
		newClient.setClientNumber(numberOfClients);
		increaseNumberOfClients();
	}

	private void increaseNumberOfClients() {
		numberOfClients++;
	}
	
	public void saveVote(int seqId) {
		CommunicateToClient ctc = voteToClients.get(seqId);
		ctc.setVote(true);
	}

	public int getNumberOfClients() {
		return numberOfClients;
	}
	
	public BigInteger getN() {
		return n;
	}

	public void setN(BigInteger n) {
		this.n = n;
	}

	public BigInteger getE() {
		return e;
	}

	public void setE(BigInteger e) {
		this.e = e;
	}

	public BigInteger getD() {
		return d;
	}

	public void setD(BigInteger d) {
		this.d = d;
	}

	public CommunicationToVS getComToVs() {
		return comToVs;
	}
	
	public Question getQ() {
		return q;
	}

	public Log getLog() {
		return log;
	}

	public void setLog(Log log) {
		this.log = log;
	}
	
	public SecretKey getMasterKey() {
		return masterKey;
	}

	public void setMasterKey(SecretKey masterKey) {
		this.masterKey = masterKey;
	}
}
