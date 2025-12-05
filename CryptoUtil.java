import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;

public class CryptoUtil {
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12; // GCM standard
    private static final int KEY_BYTES = 32; // 256 bits
    private static final int PBKDF2_ITERS = 65536;
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERS, KEY_BYTES * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGO);
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static class CipherResult {
        public final byte[] iv;
        public final byte[] cipherText;
        public CipherResult(byte[] iv, byte[] cipherText) { this.iv = iv; this.cipherText = cipherText; }
    }

    public static CipherResult encrypt(byte[] plain, SecretKey key) throws Exception {
        byte[] iv = new byte[IV_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ct = cipher.doFinal(plain);
        return new CipherResult(iv, ct);
    }

    public static byte[] decrypt(byte[] cipherText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(cipherText);
    }
}
