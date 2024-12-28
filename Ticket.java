import javax.crypto.SecretKey;
import java.time.LocalDateTime;

public class Ticket {
    private String clientId;
    private String serverId;
    private SecretKey sessionKey;
    private LocalDateTime expirationTime;
    private byte[] clientEncryptedKey;
    private byte[] serverEncryptedKey;

    public Ticket(String clientId, String serverId, SecretKey sessionKey, LocalDateTime expirationTime) {
        this.clientId = clientId;
        this.serverId = serverId;
        this.sessionKey = sessionKey;
        this.expirationTime = expirationTime;
    }

    public void setEncryptedKeys(byte[] clientEncryptedKey, byte[] serverEncryptedKey) {
        this.clientEncryptedKey = clientEncryptedKey;
        this.serverEncryptedKey = serverEncryptedKey;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    // Getters
    public String getClientId() { return clientId; }
    public String getServerId() { return serverId; }
    public SecretKey getSessionKey() { return sessionKey; }
    public byte[] getClientEncryptedKey() { return clientEncryptedKey; }
    public byte[] getServerEncryptedKey() { return serverEncryptedKey; }
    public LocalDateTime getExpirationTime() { return expirationTime; }
} 