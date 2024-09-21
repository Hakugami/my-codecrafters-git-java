package command.impl;

import command.GitCommand;
import model.GitObject;
import util.TreeBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

class WriteTreeCommand implements GitCommand {
    @Override
    public void execute(String[] args) throws IOException, NoSuchAlgorithmException {
        byte[] treeContent = TreeBuilder.buildTree(Path.of("."));
        GitObject treeObject = new GitObject("tree", treeContent);
        String hash = treeObject.write();
        System.out.println(hash);
    }
}

