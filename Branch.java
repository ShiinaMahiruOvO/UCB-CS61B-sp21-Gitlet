package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gitlet.Utils.*;

public class Branch {

    public static void createBranch(String branchName, String currentCommitID) {
        File branchFile = join(Repository.HEADS_DIR, branchName);
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        writeContents(branchFile, currentCommitID);
    }

    // Update the current commit of the given branch to the given commit.
    public static void updateBranch(String branchName, String newCommitID) {
        File branchFile = join(Repository.HEADS_DIR, branchName);
        writeContents(branchFile, newCommitID);
    }

    public static String getCurrentBranchName() {
        return readContentsAsString(Repository.HEAD);
    }

    public static String getCurrentCommitID() {
        return getBranchCurrentCommitID(getCurrentBranchName());
    }

    public static File getBranchFile(String fileName) {
        return join(Repository.HEADS_DIR, fileName.replace("/", "_"));
    }

    public static String decodeBranchName(String escapedName) {
        return escapedName.replace("_", "/");
    }

    public static String getBranchCurrentCommitID(String branchName) {
        return readContentsAsString(getBranchFile(branchName));
    }

    // Checkout all the files in the given branch.
    // (Note that branch is a pointer to a commit.)
    public static void checkOutBranchFiles(String targetBranchName) {
        String targetBranchCommitID = getBranchCurrentCommitID(targetBranchName);
        checkOutCommit(targetBranchCommitID);
    }

    // Checkout all the files in the given commit.
    public static void checkOutCommit(String targetCommitID) {
        Commit currentCommit = Commit.findCommit(getCurrentCommitID());
        Commit targetCommit = Commit.findCommit(targetCommitID);
        Map<String, String> currentFileMap = currentCommit.getFileNameToBlobID();
        Map<String, String> targetFileMap = targetCommit.getFileNameToBlobID();

        List<String> untrackedFiles = new ArrayList<>();
        for (String file : currentCommit.getUntrackedFiles()) {
            if (targetFileMap.containsKey(file)) {
                untrackedFiles.add(file);
            }
        }
        if (!untrackedFiles.isEmpty()) {
            System.out.println("`There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }

        for (String fileName : currentFileMap.keySet()) {
            if (!targetFileMap.containsKey(fileName)) {
                restrictedDelete(fileName);
            }
        }
        for (String fileName : targetFileMap.keySet()) {
            String blobID = targetFileMap.get(fileName);
            byte[] blobContent = Blob.getBlobContent(blobID);
            File targetFile = join(Repository.CWD, fileName);
            writeContents(targetFile, blobContent);
        }

        StagingArea stagingArea = StagingArea.getStagingArea();
        stagingArea.clear();
        stagingArea.saveStagingArea();
    }

    // Use BFS to find the split point when merging two branches.
    public static String findSplitPoint(String branchCommitID) {
        Set<String> visitedFromCurrent = new HashSet<>();
        Set<String> visitedFromTarget = new HashSet<>();
        Queue<String> currentQueue = new LinkedList<>();
        Queue<String> targetQueue = new LinkedList<>();

        String currentCommitID = getCurrentCommitID();
        currentQueue.add(currentCommitID);
        targetQueue.add(branchCommitID);

        while (!currentQueue.isEmpty() || !targetQueue.isEmpty()) {
            String commitID = tryVisitNext(currentQueue,
                    visitedFromCurrent, visitedFromTarget);
            if (commitID != null) {
                return commitID;
            }

            commitID = tryVisitNext(targetQueue,
                    visitedFromTarget, visitedFromCurrent);
            if (commitID != null) {
                return commitID;
            }
        }

        return null;
    }

    private static String tryVisitNext(Queue<String> queue,
                                       Set<String> visitedSelf, Set<String> visitedOther) {
        while (!queue.isEmpty()) {
            String commitID = queue.poll();

            if (!visitedSelf.add(commitID)) {
                continue;
            }
            if (visitedOther.contains(commitID)) {
                return commitID;
            }

            Commit commit = Commit.findCommit(commitID);
            if (commit == null) {
                continue;
            }
            if (commit.getParentCommitID() != null) {
                queue.add(commit.getParentCommitID());
            }
            if (commit.getSecondParentCommitID() != null) {
                queue.add(commit.getSecondParentCommitID());
            }
        }
        return null;
    }

    public static boolean mergeBranch(String branchName, String splitPointID)  {
        boolean hasConflict = false;
        String branchCommitID = Branch.getBranchCurrentCommitID(branchName);
        String currentCommitID = Branch.getCurrentCommitID();

        Commit splitCommit = Commit.findCommit(splitPointID);
        Commit currentCommit = Commit.findCommit(currentCommitID);
        Commit branchCommit = Commit.findCommit(branchCommitID);
        Map<String, String> splitFiles = splitCommit.getFileNameToBlobID();
        Map<String, String> currentFiles = currentCommit.getFileNameToBlobID();
        Map<String, String> branchFiles = branchCommit.getFileNameToBlobID();
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(splitFiles.keySet());
        allFiles.addAll(currentFiles.keySet());
        allFiles.addAll(branchFiles.keySet());

        for (String fileName : allFiles) {
            String splitBlobID = splitFiles.getOrDefault(fileName, null);
            String currentBlobID = currentFiles.getOrDefault(fileName, null);
            String branchBlobID = branchFiles.getOrDefault(fileName, null);
            if (Objects.equals(currentBlobID, branchBlobID)) {
                continue;
            }

            boolean inSplit = splitBlobID != null;
            boolean inCurrent = currentBlobID != null;
            boolean inGiven = branchBlobID != null;
            boolean currentModified = !Objects.equals(splitBlobID, currentBlobID);
            boolean branchModified = !Objects.equals(splitBlobID, branchBlobID);

            if (!inSplit) { // if not present at the split point,
                // only present at the current branch, do nothing.

                if (inGiven && !inCurrent) {
                    // only present at the given branch, checkout and stage.
                    Repository.checkOutWithCommitIDAndFileName(branchCommitID, fileName);
                    Repository.add(fileName);
                } else if (inGiven && inCurrent
                        && !Objects.equals(currentBlobID, branchBlobID)) {
                    hasConflict = true;
                    System.out.println("Encountered a merge conflict.");
                    handleConflict(fileName, currentBlobID, branchBlobID);
                }
            } else { // if present at the split point,
                // only deleted in the current branch, do nothing
                // only modified in the current branch, do nothing

                if (!inGiven && !currentModified) {
                    // deleted in the given branch, not modified in the current branch, remove it
                    Repository.remove(fileName);
                } else if ((!inGiven && currentModified) || (!inCurrent && branchModified)) {
                    hasConflict = true;
                    System.out.println("Encountered a merge conflict.");
                    handleConflict(fileName, currentBlobID, branchBlobID);
                } else if (branchModified && !currentModified) {
                    // modified in the given branch, not modified in the current branch,
                    // checkout and stage it.
                    Repository.checkOutWithCommitIDAndFileName(branchCommitID, fileName);
                    Repository.add(fileName);
                } else if (branchModified && currentModified
                        && !Objects.equals(currentBlobID, branchBlobID)) {
                    hasConflict = true;
                    System.out.println("Encountered a merge conflict.");
                    handleConflict(fileName, currentBlobID, branchBlobID);
                }
            }
        }
        return hasConflict;
    }

    private static void handleConflict(String fileName,
                                       String currentBlobID, String branchBlobID) {
        String currentContent = (currentBlobID != null)
                ? new String(Blob.getBlobContent(currentBlobID), StandardCharsets.UTF_8)
                : "";
        String branchContent = (branchBlobID != null)
                ? new String(Blob.getBlobContent(branchBlobID), StandardCharsets.UTF_8)
                : "";

        String conflictContent = "<<<<<<< HEAD\n"
                + currentContent
                + "=======\n"
                + branchContent
                + ">>>>>>>\n";
        File conflictFile = join(Repository.CWD, fileName);
        writeContents(conflictFile, conflictContent);
        Repository.add(fileName);
    }
}
