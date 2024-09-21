import java.io.IOException;

class LsTreeCommand implements GitCommand {
    @Override
    public void execute(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: ls-tree <tree-hash>");
            return;
        }
        String hash = args[1];
        GitObject treeObject = GitObject.read(hash);
        if (!"tree".equals(treeObject.type())) {
            System.out.println("Not a tree object");
            return;
        }
        TreeParser.parseFileNames(treeObject.content()).forEach(System.out::println);
    }
}