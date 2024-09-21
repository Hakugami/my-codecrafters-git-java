import java.io.IOException;
import java.security.NoSuchAlgorithmException;

interface GitCommand {
    void execute(String[] args) throws IOException, NoSuchAlgorithmException;
}
