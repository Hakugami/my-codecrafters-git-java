package model;

import java.util.Objects;

public record Commit(String author, String message, String treeHash, String parentHash, String commitDate, String committerInfo) {

    @Override
    public String toString() {
        StringBuilder commitBuilder = new StringBuilder();

        // Add tree hash
        commitBuilder.append("tree ").append(treeHash).append("\n");

        // Add parent hash if present
        if (Objects.nonNull(parentHash) && !parentHash.isEmpty()) {
            commitBuilder.append("parent ").append(parentHash).append("\n");
        }

        // Add author and committer info
        commitBuilder.append("author ").append(author).append(" ").append(commitDate).append("\n");
        commitBuilder.append("committer ").append(committerInfo).append(" ").append(commitDate).append("\n\n");

        // Add the commit message
        commitBuilder.append(message).append("\n");

        return commitBuilder.toString();
    }
}
