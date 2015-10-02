package numberTheory;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MasterKeyGenerator {
	public static SecretKey generate(){
		SecretKey sss = null;
		try {
			sss = KeyGenerator.getInstance("AES").generateKey();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String masterKey = Base64.getEncoder().encodeToString(sss.getEncoded());
		System.out.println(masterKey);
		byte[] b = Base64.getDecoder().decode(masterKey);
		SecretKey sec = new SecretKeySpec(b, 0, b.length, "AES");
		return sec;
	}
}
