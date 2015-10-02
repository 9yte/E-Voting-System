package numberTheory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * https://gist.github.com/bricef/2436364
 * 
 * @author negar
 * 
 */
public class AES {
	static byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    static IvParameterSpec ivspec = new IvParameterSpec(iv);


	public static byte[] encryptCBC(String plainText, SecretKey encryptionKey)
			throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivspec);
		return cipher.doFinal(plainText.getBytes("UTF-8"));
	}

	public static byte[] encryptECB(String plainText, SecretKey encryptionKey)
			throws Exception {
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
		cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivspec);
		return cipher.doFinal(plainText.getBytes("UTF-8"));
	}

	public static String decryptCBC(byte[] cipherText, SecretKey encryptionKey)
			throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		cipher.init(Cipher.DECRYPT_MODE, encryptionKey, ivspec);
		return new String(cipher.doFinal(cipherText), "UTF-8");
	}

	public static String decryptECB(byte[] cipherText, SecretKey encryptionKey)
			throws Exception {
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
		cipher.init(Cipher.DECRYPT_MODE, encryptionKey, ivspec);
		return new String(cipher.doFinal(cipherText), "UTF-8");
	}
}