package model;

public record TreeEntry(TreeEntryMode mode , String name, String hash) implements Comparable<TreeEntry> {
    @Override
    public int compareTo(TreeEntry o) {
        return name.compareTo(o.name);
    }

}
