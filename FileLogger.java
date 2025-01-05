import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger {
    private final String logFile;
    private final DateTimeFormatter formatter;

    public FileLogger(String logFile) {
        this.logFile = logFile;
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public void log(String message) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            String timestamp = LocalDateTime.now().format(formatter);
            String logMessage = timestamp + " - " + message + "\n";
            bw.write(logMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}