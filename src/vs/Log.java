package vs;

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
			File file = new File("src/vs/enc-log");
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
			File file = new File("src/vs/log");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false);
			BufferedWriter writer = new BufferedWriter(fstream);
			File f = new File("src/vs/enc-log");
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
	
	public void logAsInfo(BigInteger e, BigInteger n) {
		addToLog("AS has connected");
		addToLog("AS INFO:");
		String info = "public key: (e: " + e + ", n: " + n + ")";
		addToLog(info);
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
	
	public void logVote(String vote, String sign) {
		String m = "Vote Received:";
		addToLog(m);
		m = "vote: " + Integer.parseInt(vote + 1);
		addToLog(m);
		m = "sign: " + sign;
		addToLog(m);
	}
	
	public void endOfPacket(){
		addToLog("----end of details----");
	}
	
	public void logQuestion(Question q) {
		String s = "Question has received from AS Server:";
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
