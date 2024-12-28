import javax.crypto.*;
import java.security.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class HKS {
    public static class ClientInfo {
        String clientId;
        String password;
        KeyPair keyPair;
        private Ticket ticket;

        public ClientInfo(String clientId, String password, KeyPair keyPair) {
            this.clientId = clientId;
            this.password = password;
            this.keyPair = keyPair;
        }

        public void setTicket(Ticket ticket) {
            this.ticket = ticket;
        }

        public Ticket getTicket() {
            return ticket;
        }

        public String communicateWithServer(String message) throws Exception {
            // Add your server communication logic here
            // For example:
            if (ticket == null) {
                throw new Exception("No valid ticket");
            }
            return "Server processed: " + message; // Replace with actual server communication
        }

        public String getClientId() {
            return clientId;
        }
    }

    private static class ServerInfo {
        String serverId;
        KeyPair keyPair;

        public ServerInfo(String serverId, KeyPair keyPair) {
            this.serverId = serverId;
            this.keyPair = keyPair;
        }
    }

    private Map<String, ClientInfo> clients;
    private Map<String, ServerInfo> servers;
    private final String DATASET_FILE = "dataset.csv";
    private final FileLogger logger;

    public HKS() {
        clients = new HashMap<>();
        servers = new HashMap<>();
        logger = new FileLogger("log.txt");
        loadDataset();
    }

    public ClientInfo registerClient(String clientId, String password, String serverId) throws Exception {
        if (clients.containsKey(clientId)) {
            throw new Exception("Client ID already exists");
        }

        // Generate RSA key pair for client
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair clientKeyPair = keyGen.generateKeyPair();

        // Register server if it doesn't exist
        if (!servers.containsKey(serverId)) {
            KeyPair serverKeyPair = keyGen.generateKeyPair();
            servers.put(serverId, new ServerInfo(serverId, serverKeyPair));
            logger.log("New server registered: " + serverId);
        }

        // Store client information
        ClientInfo clientInfo = new ClientInfo(clientId, password, clientKeyPair);
        clients.put(clientId, clientInfo);
        
        // Save to dataset
        saveToDataset();
        logger.log("Client registered: " + clientId);

        return new ClientInfo(clientId, password, clientKeyPair);
    }

    public Ticket authenticateClient(String clientId, String password, String serverId) throws Exception {
        ClientInfo clientInfo = clients.get(clientId);
        if (clientInfo == null || !clientInfo.password.equals(password)) {
            throw new Exception("Invalid credentials");
        }

        ServerInfo serverInfo = servers.get(serverId);
        if (serverInfo == null) {
            throw new Exception("Server not found");
        }

        // Generate AES session key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey sessionKey = keyGen.generateKey();

        // Create and encrypt ticket
        Ticket ticket = new Ticket(clientId, serverId, sessionKey, LocalDateTime.now().plusMinutes(5));
        
        // Encrypt session key with client's and server's public keys
        Cipher cipher = Cipher.getInstance("RSA");
        
        cipher.init(Cipher.ENCRYPT_MODE, clientInfo.keyPair.getPublic());
        byte[] clientEncryptedKey = cipher.doFinal(sessionKey.getEncoded());
        
        cipher.init(Cipher.ENCRYPT_MODE, serverInfo.keyPair.getPublic());
        byte[] serverEncryptedKey = cipher.doFinal(sessionKey.getEncoded());

        ticket.setEncryptedKeys(clientEncryptedKey, serverEncryptedKey);
        logger.log("Ticket issued for client " + clientId + " to server " + serverId);

        return ticket;
    }

    private void loadDataset() {
        try {
            if (Files.exists(Paths.get(DATASET_FILE))) {
                // Implementation of loading data from CSV
                // This would read the stored client/server information
            }
        } catch (Exception e) {
            logger.log("Error loading dataset: " + e.getMessage());
        }
    }

    private void saveToDataset() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATASET_FILE))) {
            // Save clients
            for (ClientInfo client : clients.values()) {
                writer.println(String.format("CLIENT,%s,%s,%s,%s",
                    client.clientId,
                    client.password,
                    Base64.getEncoder().encodeToString(client.keyPair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(client.keyPair.getPrivate().getEncoded())
                ));
            }
            // Save servers
            for (ServerInfo server : servers.values()) {
                writer.println(String.format("SERVER,%s,%s,%s",
                    server.serverId,
                    Base64.getEncoder().encodeToString(server.keyPair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(server.keyPair.getPrivate().getEncoded())
                ));
            }
        } catch (IOException e) {
            logger.log("Error saving dataset: " + e.getMessage());
        }
    }
} 