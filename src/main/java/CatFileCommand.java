import java.io.IOException;

class CatFileCommand implements GitCommand {
    @Override
    public void execute(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: cat-file <object-hash>");
            return;
        }
        String hash = args[1];
        GitObject object = GitObject.read(hash);
        String content = new String(object.content());
        System.out.print(content);
    }
}
