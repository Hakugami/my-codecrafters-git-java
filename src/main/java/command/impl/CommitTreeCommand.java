package command.impl;

import command.GitCommand;
import model.AuthorSignature;
import model.Commit;
import model.GitObject;
import util.Git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CommitTreeCommand implements GitCommand {
    private static final String AUTHOR_NAME = "author";  // Replace with dynamic author if needed
    private static final String AUTHOR_EMAIL = "author@example.com";  // Replace with actual email if needed

    @Override
    public void execute(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length < 1) {
            System.out.println("Usage: commit-tree <tree-hash> [-p <parent-hash>] [-m <message>]");
            return;
        }

        // Extracting the tree hash
        String treeHash = args[0];
        if (treeHash == null || treeHash.isEmpty()) {
            System.out.println("Error: Tree hash is required.");
            return;
        }

        String parentHash = null;
        String message = null;

        // Parse additional options: -p for parent hash and -m for message
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-p") && i + 1 < args.length) {
                parentHash = args[i + 1];
                i++;  // Skip the next argument (parent hash)
            } else if (args[i].equals("-m") && i + 1 < args.length) {
                message = args[i + 1];
                i++;  // Skip the next argument (message)
            }
        }

        // Validate the commit message
        if (message == null || message.isEmpty()) {
            System.out.println("Error: Commit message is required.");
            return;
        }

        // Use current date-time in proper format for Git commits
        String date = getCurrentDateTime();



        AuthorSignature authorSignature = new AuthorSignature(AUTHOR_NAME, AUTHOR_EMAIL, ZonedDateTime.now());

        // Create a new commit object, passing all necessary fields
        Commit commit = new Commit(treeHash, parentHash, authorSignature, authorSignature, message);

        // Convert the commit object to bytes using UTF-8
        byte[] content = commit.toString().getBytes(StandardCharsets.UTF_8);

        Git git = new Git(Path.of("."));

        // Create and write the Git object (commit)
        String commitHash = git.writeObject(commit);

        // Output only the SHA-1 hash, no additional text
        System.out.println(commitHash);
    }

    // Helper method to get the current date-time in Git's format, including timezone
    private String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
        return OffsetDateTime.now().format(formatter);
    }
}
