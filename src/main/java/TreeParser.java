import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

class TreeParser {
    public static List<String> parse(byte[] treeContent) {
        List<String> entries = new ArrayList<>();
        int i = 0;
        while (i < treeContent.length) {
            int spaceIndex = indexOf(treeContent, (byte) ' ', i);
            int nullIndex = indexOf(treeContent, (byte) 0, spaceIndex);
            String mode = new String(treeContent, i, spaceIndex - i);
            String name = new String(treeContent, spaceIndex + 1, nullIndex - spaceIndex - 1);
            String hash = HexFormat.of().formatHex(Arrays.copyOfRange(treeContent, nullIndex + 1, nullIndex + 21));
            entries.add(mode + " " + name + " " + hash);
            i = nullIndex + 21;
        }
        return entries;
    }

    public static List<String> parseFileNames(byte[] treeContent) {
        List<String> entries = new ArrayList<>();
        int i = 0;
        while (i < treeContent.length) {
            int spaceIndex = indexOf(treeContent, (byte) ' ', i);
            int nullIndex = indexOf(treeContent, (byte) 0, spaceIndex);
            String name = new String(treeContent, spaceIndex + 1, nullIndex - spaceIndex - 1);
            entries.add(name);
            i = nullIndex + 21;
        }
        return entries;
    }

    private static int indexOf(byte[] array, byte target, int start) {
        for (int i = start; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }
}