package command.impl;

import command.GitCommand;
import model.GitObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class HashObjectCommand implements GitCommand {
    @Override
    public void execute(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length < 1) {
            System.out.println("Usage: hash-object <file-path>");
            return;
        }
        String filePath = args[1];
        byte[] content = Files.readAllBytes(Path.of(filePath));
        GitObject blob = new GitObject("blob", content);
        String hash = blob.write();
        System.out.println(hash);
    }
}