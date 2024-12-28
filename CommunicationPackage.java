public class CommunicationPackage {
    private String clientId;
    private String serverId;
    private byte[] encryptedSessionKey;
    private byte[] encryptedMessage;

    public CommunicationPackage(String clientId, String serverId, 
                              byte[] encryptedSessionKey, byte[] encryptedMessage) {
        this.clientId = clientId;
        this.serverId = serverId;
        this.encryptedSessionKey = encryptedSessionKey;
        this.encryptedMessage = encryptedMessage;
    }

    // Getters
    public String getClientId() { return clientId; }
    public String getServerId() { return serverId; }
    public byte[] getEncryptedSessionKey() { return encryptedSessionKey; }
    public byte[] getEncryptedMessage() { return encryptedMessage; }
} 