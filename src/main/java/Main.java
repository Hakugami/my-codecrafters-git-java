import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <command> [<args>]");
            System.exit(1);
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        GitCommand gitCommand = GitCommandFactory.getCommand(command);
        if (gitCommand == null) {
            System.out.println("Unknown command: " + command);
            System.exit(1);
        }

        try {
            gitCommand.execute(commandArgs);
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }
    }
}