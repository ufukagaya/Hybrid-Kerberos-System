import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class Server {
    private final String serverId;
    private final FileLogger logger;
    private Ticket ticket;
    private final KDC kdc;

    public Ticket getTicket() {
        return this.ticket;
    }

    public Server(String serverId, KDC kdc) {
        this.serverId = serverId;
        this.logger = new FileLogger("log.txt");
        this.kdc = kdc;
    }
    public String getServerId(){ return this.serverId; }
    public void setTicket(Ticket ticket){ this.ticket = ticket;}

    private byte[] decryptSessionKey() throws Exception {
        try {
            byte[] decryptedKey = this.kdc.decryptSessionKey(serverId, ticket.getEncryptedKey(), false);
            logger.log("Server " + serverId + " decrypted session key successfully");
            return decryptedKey;
        } catch (Exception e) {
            logger.log("Error decrypting session key: " + e.getMessage());
            throw new Exception("Failed to decrypt session key");
        }
    }

    public byte[] getMessage(byte[] encryptedMessage){
        try {
            byte[] decryptedServerSessionKey = decryptSessionKey();
            SecretKeySpec sessionKeySpec = new SecretKeySpec(decryptedServerSessionKey, "AES");

            logger.log("Server attempting to decrypt received message...");
            Cipher decryptCipher = Cipher.getInstance("AES");
            decryptCipher.init(Cipher.DECRYPT_MODE, sessionKeySpec);
            byte[] decryptedMessage = decryptCipher.doFinal(encryptedMessage);

            String decryptedMessageString = new String(decryptedMessage);
            logger.log("Server received: " + decryptedMessageString);
            logger.log("Message decrypted by the server successfully.");
            String sendBackMessage = "Message: " + decryptedMessageString + " is recieved.";
            logger.log("Server preparing response message...");

            Cipher encryptCipher = Cipher.getInstance("AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, sessionKeySpec);

            byte[] returnedMessage = encryptCipher.doFinal(sendBackMessage.getBytes());
            logger.log("Server response encrypted and ready to send.");

            return returnedMessage;
        }catch (Exception e) {
            logger.log("Error processing message: " + e.getMessage());
        }
        return "".getBytes();
    }

    public boolean tryToCommunicate(byte[] clientSessionKey) {
        try {
            if (ticket.isExpired()) {
                ticket.resetExpiration();
                logger.log("Server ticket has expired and been reset");
                return false;
            }

            logger.log(serverId + " is ready to receive messages.");
            byte[] decryptedServerSessionKey = decryptSessionKey();

            if(Arrays.equals(clientSessionKey, decryptedServerSessionKey)){
                logger.log("Server: Session key is now active for communication.");
                return true;
            }else{
                logger.log("Server: Session key verification failed.");
                return false;
            }
        } catch (Exception e) {
            logger.log("Error! Session keys do not match: " + e.getMessage());
            return false;
        }
    }
} 