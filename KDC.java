import javax.crypto.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class KDC {

    private final Map<String, Client> clients;
    private final Map<String, Server> servers;
    private final String DATASET_FILE = "dataset.csv";
    private final FileLogger logger;

    public KDC() {
        clients = new HashMap<>();
        servers = new HashMap<>();
        logger = new FileLogger("log.txt");
        loadDataset();
    }

    public byte[] decryptSessionKey(String id, byte[] encryptedKey, boolean isClient) throws Exception {
        PrivateKey privKey;
        if(isClient){
            privKey = getPrivateKey(id, "CLIENT");
        }else{
            privKey = getPrivateKey(id, "SERVER");
        }
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(encryptedKey);
    }

    public void registerClient(String clientId, String password, String serverId) throws Exception {
        if (isClientServerRegistered(clientId, serverId)) {
            throw new Exception("This client-server pair already exists in the system");
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair clientKeyPair = keyGen.generateKeyPair();

        Server serverInfo;
        PrivateKey serverPrivateKey = null;
        PublicKey serverPublicKey = null;

        if (!servers.containsKey(serverId)) {
            KeyPair serverKeyPair = keyGen.generateKeyPair();
            serverInfo = new Server(serverId, this);
            serverPrivateKey = serverKeyPair.getPrivate();
            serverPublicKey = serverKeyPair.getPublic();
            servers.put(serverId, serverInfo);
            logger.log("New server registered: " + serverId);
        }

        // Store client information
        Client clientInfo = new Client(clientId, password, this);
        clients.put(clientId, clientInfo);

        // Save to dataset
        saveToDataset(clientInfo, clientKeyPair.getPrivate(), clientKeyPair.getPublic(),
                serverPrivateKey, serverPublicKey, serverId);
        logger.log("Client " + clientId + " registered with server " + serverId);

    }

    private boolean isClientServerRegistered(String clientId, String serverId) {
        try {
            File file = new File(DATASET_FILE);
            if (!file.exists()) {
                return false;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals("CLIENT")) {
                    if (parts[1].equals(clientId) & parts[5].equals(serverId)) {
                        reader.close();
                        logger.log("Client " + clientId + " already exists in the system");
                        return true;
                    }
                }
            }
            reader.close();
            return false;
        } catch (IOException e) {
            logger.log("Error checking dataset: " + e.getMessage());
            return false;
        }
    }



    public boolean authenticateClient(String clientId, String password, String serverId) throws Exception {
        Client clientInfo = clients.get(clientId);
        if (clientInfo == null || !clientInfo.getPassword().equals(password)) {
            throw new Exception("Invalid credentials");
        }

        Server serverInfo = servers.get(serverId);
        if (serverInfo == null) {
            throw new Exception("Server not found");
        }

        // Create ticket
        TicketGrant.grant(clientInfo, serverInfo, this);

        logger.log("Ticket issued for client " + clientId + " to server " + serverId);
        return true;
    }

    private void loadDataset() {
        try {
            if (Files.exists(Paths.get(DATASET_FILE))) {
                BufferedReader reader = new BufferedReader(new FileReader(DATASET_FILE));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts[0].equals("CLIENT")) {
                        String clientId = parts[1];
                        String password = parts[2];
                        String associatedServerId = parts[5];
                        Client client = new Client(clientId, password, this);
                        clients.put(clientId, client);
                        logger.log("Loaded client: " + clientId + " associated with server: " + associatedServerId);
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            logger.log("Error loading dataset: " + e.getMessage());
        }
    }

    private void saveToDataset(Client clientInfo, PrivateKey clientPrivateKey,
                               PublicKey clientPublicKey,
                               PrivateKey serverPrivateKey, PublicKey serverPublicKey, String serverId) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATASET_FILE, true))) {
            // Save client info
            String clientPrivateKeyBase64 = Base64.getEncoder().encodeToString(clientPrivateKey.getEncoded());
            String clientPublicKeyBase64 = Base64.getEncoder().encodeToString(clientPublicKey.getEncoded());

            writer.println(String.format("CLIENT,%s,%s,%s,%s,%s",
                    clientInfo.getClientId(),
                    clientInfo.getPassword(),
                    clientPublicKeyBase64,
                    clientPrivateKeyBase64,
                    serverId
            ));

            if(serverPrivateKey != null){
                // Save server info
                String serverPrivateKeyBase64 = Base64.getEncoder().encodeToString(serverPrivateKey.getEncoded());
                String serverPublicKeyBase64 = Base64.getEncoder().encodeToString(serverPublicKey.getEncoded());

                writer.println(String.format("SERVER,%s,%s,%s",
                        serverId,
                        serverPublicKeyBase64,
                        serverPrivateKeyBase64
                ));
            }
            logger.log("Registration data saved to dataset successfully");
        } catch (IOException e) {
            logger.log("Error saving dataset: " + e.getMessage());
        }
    }

    private PrivateKey getPrivateKey(String id, String type) throws Exception{
        try (BufferedReader reader = new BufferedReader(new FileReader(DATASET_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts[0].equals(type) && parts[1].equals(id)) {
                    String privateKeyBase64 = parts[0].equals("SERVER") ? parts[3] : parts[4];

                    byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);

                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    return keyFactory.generatePrivate(spec);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading dataset: " + e.getMessage());
        }
        return null;
    }

    public PublicKey getPublicKey(String id, String type) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATASET_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts[0].equals(type) && parts[1].equals(id)) {
                    String publicKeyBase64 = (type.equals("SERVER")) ? parts[2] : parts[3];

                    byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

                    X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    return keyFactory.generatePublic(spec);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading dataset: " + e.getMessage());
        }
        return null;
    }

    public Server getServer(String serverId) {
        return servers.get(serverId);
    }
    public Client getClient(String clientID) {
        return clients.get(clientID);
    }
}
