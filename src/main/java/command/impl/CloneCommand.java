package command.impl;

import command.GitCommand;
import model.*;
import protocol.client.GitHttpClient;
import protocol.model.DeltaInstruction;
import protocol.model.PackObject;
import protocol.util.PacketParser;
import serializer.TreeSerializer;
import util.Git;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CloneCommand implements GitCommand {
    private Queue<PackObject.Deltafied> deltafiedQueue = new LinkedList<>();

    @Override
    public void execute(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: clone <repository-url> [path]");
            return;
        }

        final String repositoryUrl = args[0];

        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            System.out.println("Error: Repository URL is required.");
            return;
        }

        if (!repositoryUrl.matches("^https?://.*")) {
            System.err.println("Error: Invalid repository URL format. URL must start with http:// or https://");
            return;
        }

        Path path = args.length > 1 ? Path.of(args[1]) : Path.of(".");

        try {
            Git.clone(URI.create(repositoryUrl), path);

        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid repository URL provided.");
        } catch (Exception e) {
            System.err.println("An error occurred while cloning the repository: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
