package numberTheory;

import java.math.BigInteger;
import java.util.Random;

public class RSA {
	private int keyLength;
	private BigInteger ONE = new BigInteger("1");
	private BigInteger n;
	private BigInteger phiN;
	private BigInteger e;
	private BigInteger d;

	public static void main(String[] args) {
		new RSA(1024);
	}

	public RSA(int keyLength) {
		this.keyLength = keyLength;
		createRSAParameter();
//		generateRSAParameter();
		generateRSAKeys();
//		printRSAParameterAndKeys();
	}

	/**
	 * @definition this method create e and d!
	 */
	private void generateRSAKeys() {
		int len = phiN.bitLength();
		while (true) {
			e = new BigInteger(len, new Random());
			if (e.gcd(phiN).equals(ONE))
				break;
		}
		d = e.modInverse(phiN);
	}

	/**
	 * @definition this method create n according to values gained from
	 *             generateRSAParameter method in previous runs!
	 */
	private void createRSAParameter() {
		n = new BigInteger(
				"154708760804741478699270858412306050407869604835401520300925307404042527122732315740512499491185475124661530868316140842395795510896157449681769149263673301117017439724686243993908371355402621230550971063013425083366803034442405776416543043914453158676589678942134363264778397872582878566134510743957457034729");
		phiN = new BigInteger(
				"154708760804741478699270858412306050407869604835401520300925307404042527122732315740512499491185475124661530868316140842395795510896157449681769149263673276190799332092440029255953710032774091854137528531959413983419644571076547008265295923437471767056203709814122155806998581871760263814940804022748509017476");
	}

	/**
	 * @definition this method generate n and calculate phiN! needs to run only
	 *             once!
	 */
	@SuppressWarnings("unused")
	private void generateRSAParameter() {
		CycleGroup c1 = new CycleGroup((keyLength / 2) - 1);
		CycleGroup c2 = new CycleGroup((keyLength / 2) - 1);
		BigInteger p = c1.getP();
		BigInteger q = c2.getP();
		n = p.multiply(q);
		phiN = (p.subtract(ONE)).multiply((q.subtract(ONE)));
		System.out.println(n);
		System.out.println(phiN);
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
}
