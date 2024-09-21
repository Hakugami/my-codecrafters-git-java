package command;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface GitCommand {
    void execute(String[] args) throws IOException, NoSuchAlgorithmException;
}
