public class Client {
    private String clientId;
    private Ticket ticket;

    public Client(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() { return clientId; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public String communicateWithServer(String message) throws Exception {
        // Implementation here
        return "Server response";
    }
} 