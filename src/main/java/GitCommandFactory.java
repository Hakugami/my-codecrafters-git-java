import java.util.HashMap;
import java.util.Map;

class GitCommandFactory {
    private static final Map<String, GitCommand> commands = new HashMap<>();

    static {
        commands.put("init", new InitCommand());
        commands.put("cat-file", new CatFileCommand());
        commands.put("hash-object", new HashObjectCommand());
        commands.put("ls-tree", new LsTreeCommand());
        commands.put("write-tree", new WriteTreeCommand());
    }

    public static GitCommand getCommand(String commandName) {
        return commands.get(commandName);
    }
}
