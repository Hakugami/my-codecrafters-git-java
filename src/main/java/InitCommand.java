import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class InitCommand implements GitCommand {
    @Override
    public void execute(String[] args) throws IOException {
        Path gitDir = Path.of(".git");
        Files.createDirectories(gitDir.resolve("objects"));
        Files.createDirectories(gitDir.resolve("refs"));
        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/main\n");
        System.out.println("Initialized git directory");
    }
}
