import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Client {
    private final String clientId;
    private final String password;
    private Ticket ticket;
    private final KDC kdc;
    private final FileLogger logger;


    public Client(String clientId, String password, KDC kdc) {
        this.clientId = clientId;
        this.password = password;
        this.kdc = kdc;
        this.logger = new FileLogger("log.txt");
    }

    public String getClientId() { return this.clientId; }
    public Ticket getTicket() { return this.ticket; }
    public String getPassword(){ return this.password; }

    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public String communicateWithServer(String message) throws Exception {
        if (ticket == null) {
            throw new Exception("No valid ticket");
        }

        if (ticket.isExpired()) {
            ticket = null;
            throw new Exception("Ticket is expired. Please try to login again.");
        }

        logger.log("Decrypting session key using KDC...");
        byte[] decryptedSessionKey = kdc.decryptSessionKey(clientId, ticket.getEncryptedKey(), true);
        logger.log("Session key decrypted successfully");
        Server server = getServerById(ticket.getServerId());
        if (server == null) {
            logger.log("Error: Server not found");
            throw new Exception("Server not found for ID: " + ticket.getServerId());
        }

        logger.log("Verifying session key with server...");
        boolean canCommunicate = server.tryToCommunicate(decryptedSessionKey);
        if (!canCommunicate) {
            throw new Exception("No response from server");
        }else{
            logger.log("Preparing to encrypt message with AES session key...");
            SecretKeySpec sessionKeySpec = new SecretKeySpec(decryptedSessionKey, "AES");
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, sessionKeySpec);

            byte[] encryptedMessage = aesCipher.doFinal(message.getBytes());
            logger.log("Message encrypted successfully");
            logger.log("Sending encrypting message to server...");
            byte[] returned = server.getMessage(encryptedMessage);
            logger.log("Decrypting server response...");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, sessionKeySpec);

            byte[] returnedMessage = cipher.doFinal(returned);
            String decryptedFinalMessage = new String(returnedMessage);
            logger.log("Server response dercypted successfully");
            return decryptedFinalMessage;
        }
    }

    private Server getServerById(String serverId) {

        return kdc.getServer(serverId);
    }
} 