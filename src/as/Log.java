package as;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.SecretKey;

import numberTheory.AES;
import question.Question;

public class Log {
	private BufferedWriter writer;
	private SecretKey masterKey;

	public Log() {
		try {
			File file = new File("src/as/enc-log");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), true);
			writer = new BufferedWriter(fstream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void decryptLog() {
		try {
			File file = new File("src/as/log");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false);
			BufferedWriter writer = new BufferedWriter(fstream);
			File f = new File("src/as/enc-log");
			FileReader fr = new FileReader(f);
			BufferedReader reader = new BufferedReader(fr);
			String s;
			while((s = reader.readLine()) != null){
				BigInteger i = new BigInteger(s);
				String m = AES.decryptCBC(i.toByteArray(), masterKey);
				writer.write(m + "\n");
				writer.flush();
			}
			reader.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void logVsInfo(BigInteger e, BigInteger n) {
		addToLog("VS has connected");
		addToLog("VS INFO:");
		String info = "public key: (e: " + e + ", n: " + n + ")";
		addToLog(info);
		addToLog("------");
	}
	
	public void logClientInfo(BigInteger e, BigInteger n){
		addToLog("Client has connected");
		addToLog("Client INFO:");
		String info = "public key: (e: " + e + ", n: " + n + ")";
		addToLog(info);
		addToLog("------");
	}
	
	public void logBlindMessage(String nounce, String blind, String SSN, String s, boolean isValid){
		addToLog("Client's(" + SSN + ") blind message has received");
		addToLog("Blind Message Info:");
		addToLog("Nounce2: " + nounce);
		addToLog("Blind Message: " + blind);
		addToLog("Sign: " + s);
		if(isValid)
			addToLog("Sign is valid.");
		else
			addToLog("Sign isn't valid.");
		addToLog("------");
	}
	
	public void logVote(String SSN, String encVote, String nounce){
		addToLog("Client's(" + SSN + ") encrypted vote has received");
		addToLog("Vote Info: ");
		addToLog("Nounce2: " + nounce);
		addToLog("Encrypted vote: " + encVote);
		addToLog("------");
	}
	
	public void logAckMessage(String seq){
		addToLog("ACK message has received from VS");
		addToLog("Sequence number: " + seq);
	}
	
	public void logSSNandNounce1(String SSN, String n, String s, boolean isValid){
		addToLog("Client Nounce1 AND SSN:");
		String info = "SSN: " + SSN;
		addToLog(info);
		addToLog("Nounce1: " + n);
		addToLog("Sign: " + s);
		if(isValid)
			addToLog("Sign is valid.");
		else
			addToLog("Sign isn't valid.");
		addToLog("------");
	}

	public void logInformation(BigInteger e, BigInteger d, BigInteger n) {
		addToLog("KEYS INFO:");
		String info = "public key: (e: " + e + ", n: " + n + ")";
		addToLog(info);
		info = "private key: (d: " + d + ", n: " + n + ")";
		addToLog(info);
		addToLog("------");
	}

	public void logCurrentDateTime() {
		String time = getCurrentDateTime();
		try {
			addToLog("----------------- Voting Server has been run in " + time
					+ " -----------------");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getCurrentDateTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return (dateFormat.format(date));
	}

	private void addToLog(String message) {
		try {
			byte b[] = AES.encryptCBC(message, masterKey);
			BigInteger i = new BigInteger(b);
			writer.write(i + "\n");
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void endOfPacket(){
		addToLog("----end of details----");
	}
	
	public void logQuestion(Question q) {
		String s = "Question: ";
		addToLog(s);
		addToLog(q.getQuestion());
		int num = q.getNumberOfChoices();
		for (int i = 0; i < num; i++) {
			s = (i + 1) + ". " + q.getChoice(i).getChoice();
			addToLog(s);
		}
	}
	
	public void logDecPacket(String decPacket) {
		String m = "Plain Packet:";
		addToLog(m);
		addToLog(decPacket);
	}

	public void addPacketToLog(String packet) {
		String s = "Packet has been received -> " + getCurrentDateTime();
		addToLog(s);
		addToLog(packet);
	}

	public SecretKey getMasterKey() {
		return masterKey;
	}

	public void setMasterKey(SecretKey masterKey) {
		this.masterKey = masterKey;
	}
}
