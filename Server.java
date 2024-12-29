import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

public class Server {
    private String serverId;
    private PrivateKey privateKey; // In real implementation, this would be securely stored
    private SecretKey sessionKey;
    private final FileLogger logger;

    private KeyPair keyPair;

    public Server(String serverId, KeyPair keyPair) {
        this.serverId = serverId;
        this.keyPair = keyPair;
        this.logger = new FileLogger("log.txt");
        // In a real implementation, the private key would be loaded securely
    }

    public String communicateWithClient(CommunicationPackage pkg) throws Exception {
        if (!pkg.getServerId().equals(serverId)) {
            logger.log("Server received request for wrong server ID");
            throw new Exception("Invalid server ID");
        }

        try {
            // Decrypt session key using server's private key
            decryptSessionKey(pkg.getEncryptedSessionKey());

            // Decrypt the message using session key
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, sessionKey);
            byte[] decryptedMessage = cipher.doFinal(pkg.getEncryptedMessage());
            String message = new String(decryptedMessage);

            // Process message and create response
            String response = processMessage(message);

            // Encrypt response
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            byte[] encryptedResponse = cipher.doFinal(response.getBytes());

            logger.log("Server " + serverId + " processed request from client " + pkg.getClientId());
            return Base64.getEncoder().encodeToString(encryptedResponse);

        } catch (Exception e) {
            logger.log("Server error processing request: " + e.getMessage());
            throw new Exception("Server communication error: " + e.getMessage());
        }
    }

    private void decryptSessionKey(byte[] encryptedSessionKey) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedKey = cipher.doFinal(encryptedSessionKey);
            this.sessionKey = new SecretKeySpec(decryptedKey, "AES");
            logger.log("Server " + serverId + " decrypted session key successfully");
        } catch (Exception e) {
            logger.log("Error decrypting session key: " + e.getMessage());
            throw new Exception("Failed to decrypt session key");
        }
    }

    private String processMessage(String message) {
        // In a real implementation, this would process the client's message
        return "Server received: " + message;
    }
} 