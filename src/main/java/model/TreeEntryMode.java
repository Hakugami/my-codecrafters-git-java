package model;

import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public record TreeEntryMode(TreeEntryModeType type , int permissions) {

    public TreeEntryMode{
        if(type.isPermissionless() && permissions != 0) {
            throw new IllegalArgumentException("Permissions are not allowed for " + type);
        }
    }

    public String format() {
        return Integer.toOctalString(type.shifted() | permissions);
    }

    public static TreeEntryMode directory() {
        return new TreeEntryMode(TreeEntryModeType.DIRECTORY, 0);
    }

    public static TreeEntryMode regularFile(int permissions) {
        return new TreeEntryMode(TreeEntryModeType.REGULAR_FILE, permissions);
    }

    public static TreeEntryMode regularFile(PosixFileAttributes attributes) {
        Set<PosixFilePermission> permissions1 = attributes.permissions();

        if(permissions1.contains(PosixFilePermission.OWNER_EXECUTE) ){
            return regularFile(0755);
        }
        return regularFile(0644);
    }
}
