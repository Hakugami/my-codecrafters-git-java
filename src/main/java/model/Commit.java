package model;

import java.util.Objects;

public record Commit(String treeHash, String parentHash, AuthorSignature author,
                     AuthorSignature committer, String message) {

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
        commitBuilder.append("author ").append(author.format()).append("\n");
        commitBuilder.append("committer ").append(committer.format()).append("\n");


        // Add the commit message
        commitBuilder.append(message).append("\n");

        return commitBuilder.toString();
    }
}
