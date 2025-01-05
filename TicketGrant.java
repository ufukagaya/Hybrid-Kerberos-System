import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;

public class TicketGrant {
    public static void grant(Client clientInfo, Server serverInfo, KDC kdc) throws Exception {
        String clientId = clientInfo.getClientId();
        String serverId = serverInfo.getServerId();
        Ticket clientTicket = new Ticket(clientId, serverId, LocalDateTime.now().plusMinutes(5));
        Ticket serverTicket = new Ticket(clientId, serverId, LocalDateTime.now().plusMinutes(5));

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey sessionKey = keyGen.generateKey();

        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.ENCRYPT_MODE, kdc.getPublicKey(clientId, "CLIENT"));
        byte[] clientEncryptedKey = cipher.doFinal(sessionKey.getEncoded());

        cipher.init(Cipher.ENCRYPT_MODE, kdc.getPublicKey(serverId, "SERVER"));
        byte[] serverEncryptedKey = cipher.doFinal(sessionKey.getEncoded());

        clientTicket.setEncryptedKey(clientEncryptedKey);
        serverTicket.setEncryptedKey(serverEncryptedKey);

        clientInfo.setTicket(clientTicket);
        serverInfo.setTicket(serverTicket);
    }
}
