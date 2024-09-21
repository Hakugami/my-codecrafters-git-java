package command.impl;

import command.GitCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InitCommand implements GitCommand {
    @Override
    public void execute(String[] args) throws IOException {
        Path gitDir = args.length > 0 ? Path.of(args[0]).resolve(".git") : Path.of(".git");
        Files.createDirectories(gitDir.resolve("objects"));
        Files.createDirectories(gitDir.resolve("refs"));
        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/main\n");
        System.out.println("Initialized git directory");
    }
}
