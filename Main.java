package gitlet;

import java.io.IOException;

import static gitlet.Repository.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author George Yuan
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        Repository repository = new Repository();
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validateNumAndFormatArgs(args, 1);
                init();
                break;
            case "add":
                validateNumAndFormatArgs(args, 2);
                add(args[1]);
                break;
            case "commit":
                validateNumAndFormatArgs(args, 2);
                commit(args[1]);
                break;
            case "rm":
                validateNumAndFormatArgs(args, 2);
                remove(args[1]);
                break;
            case "log":
                validateNumAndFormatArgs(args, 1);
                log();
                break;
            case "global-log":
                validateNumAndFormatArgs(args, 1);
                globalLog();
                break;
            case "find":
                validateNumAndFormatArgs(args, 2);
                find(args[1]);
                break;
            case "status":
                validateNumAndFormatArgs(args, 1);
                status();
                break;
            case "checkout":
                handleCheckOutCall(args, repository);
                break;
            case "branch":
                validateNumAndFormatArgs(args, 2);
                branch(args[1]);
                break;
            case "rm-branch":
                validateNumAndFormatArgs(args, 2);
                removeBranch(args[1]);
                break;
            case "reset":
                validateNumAndFormatArgs(args, 2);
                reset(args[1]);
                break;
            case "merge":
                validateNumAndFormatArgs(args, 2);
                merge(args[1]);
                break;
            case "add-remote":
                validateNumAndFormatArgs(args, 3);
                addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                validateNumAndFormatArgs(args, 2);
                removeRemote(args[1]);
                break;
            case "push":
                validateNumAndFormatArgs(args, 3);
                pushRemote(args[1], args[2]);
                break;
            case "fetch":
                validateNumAndFormatArgs(args, 3);
                fetchRemote(args[1], args[2]);
                break;
            case "pull":
                validateNumAndFormatArgs(args, 3);
                pullRemote(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    private static void validateNumAndFormatArgs(String[] args, int argsNumber) {
        if (args.length != argsNumber) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (!args[0].equals("init") && !Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    private static void validateCheckOutArgs(String[] args) {
        if (!(args.length == 2 || args.length == 3 || args.length == 4)) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (args.length == 3 && !args[1].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (args.length == 4 && !args[2].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    private static void handleCheckOutCall(String[] args, Repository repository) {
        validateCheckOutArgs(args);
        if (args.length == 3) {
            checkOutWithFileName(args[2]);
        }
        if (args.length == 4) {
            checkOutWithCommitIDAndFileName(args[1], args[3]);
        }
        if (args.length == 2) {
            checkOutWithBranchName(args[1]);
        }
    }
}
