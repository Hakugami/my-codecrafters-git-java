package command.impl;

import command.GitCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class GitCommandFactory {
    private static final Map<String, Supplier<GitCommand>> commands = new HashMap<>();

    static {
        commands.put("init", InitCommand::new);
        commands.put("cat-file", CatFileCommand::new);
        commands.put("hash-object", HashObjectCommand::new);
        commands.put("ls-tree", LsTreeCommand::new);
        commands.put("write-tree", WriteTreeCommand::new);
        commands.put("commit-tree", CommitTreeCommand::new);
    }

    public static GitCommand getCommand(String commandName) {
        return commands.get(commandName).get();
    }
}
