
import java.time.LocalDateTime;

public class Ticket {
    private final String clientId;
    private final String serverId;
    private LocalDateTime expirationTime;
    private byte[] encryptedKey;

    public Ticket(String clientId, String serverId, LocalDateTime expirationTime) {
        this.clientId = clientId;
        this.serverId = serverId;
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    public void resetExpiration() {
        this.expirationTime = LocalDateTime.now().plusMinutes(5);
    }

    public String getServerId() { return serverId; }
    public byte[] getEncryptedKey() { return encryptedKey; }
    public void setEncryptedKey(byte[] encryptedKey){ this.encryptedKey = encryptedKey;}
} 