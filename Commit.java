package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *
 *  @author George Yuan
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The commit time. */
    private Date timeStamp;
    /** The parent commit's ID. */
    private String parentCommitID;
    /** The second parent commit's ID, appears when two branches merge together. */
    private String secondParentCommitID;
     /** The message of this commit. */
    private String message;
    /** The commit files and their blob IDs. */
    private Map<String, String> fileNameToBlobID;
    /** The commit ID. */
    private String commitID;
    /** The standard length of a commit ID. */
    public static final int STANDARD_COMMIT_ID_LENGTH = 40;
    /** The initial commit ID. */
    private static String initialCommitID;

    public Commit() {
        timeStamp = new Date(0);
        parentCommitID = null;
        message = "initial commit";
        fileNameToBlobID = new HashMap<>();
        commitID = sha1((Object) serialize(this));
        initialCommitID = commitID;
    }

    public Commit(String message, String parentCommitID,
                  Map<String, String> fileNameToBlobID) {
        timeStamp = new Date();
        this.parentCommitID = parentCommitID;
        this.message = message;
        this.fileNameToBlobID = fileNameToBlobID;
        commitID = sha1((Object) serialize(this));
    }

    // Only used when two branches merge together.
    private Commit(String message, String parentCommitID, String secondParentCommitID,
                   Map<String, String> fileNameToBlobID) {
        timeStamp = new Date();
        this.parentCommitID = parentCommitID;
        this.secondParentCommitID = secondParentCommitID;
        this.message = message;
        this.fileNameToBlobID = fileNameToBlobID;
        commitID = sha1((Object) serialize(this));
    }

    public void saveCommit() {
        if (!Repository.COMMITS_DIR.exists()) {
            Repository.COMMITS_DIR.mkdir();
        }
        File commitFile = join(Repository.COMMITS_DIR, commitID);
        writeObject(commitFile, this);
    }

    public static Commit findCommit(String commitID) {
        if (commitID == null) {
            return null;
        }
        File targetCommit = join(Repository.COMMITS_DIR, commitID);
        if (!targetCommit.exists()) {
            return null;
        }
        return readObject(targetCommit, Commit.class);
    }

    public static Commit findCommitWithShortID(String shortCommitID) {
        List<String> matches = new ArrayList<>();
        for (String commitID : plainFilenamesIn(Repository.COMMITS_DIR)) {
            if (commitID.startsWith(shortCommitID)) {
                matches.add(commitID);
            }
        }
        return (matches.size() == 1) ? findCommit(matches.get(0)) : null;
    }

    public static Commit createMergeCommit(String message,
                                           String parentCommitID, String mergedCommitID,
                                           Map<String, String> fileNameToBlobID) {
        if (mergedCommitID == null) {
            return new Commit(message, parentCommitID, fileNameToBlobID);
        } else {
            return new Commit(message, parentCommitID, mergedCommitID, fileNameToBlobID);
        }
    }

    // Any files are added, deleted or modified but not staged.
    public List<String> getModifiedNotStagedFiles() {
        List<String> modifiedNotStagedFiles = new ArrayList<>();
        StagingArea stagingArea = StagingArea.getStagingArea();
        Map<String, String> stageForAddition = stagingArea.getStageForAddition();
        List<String> stageForRemoval = stagingArea.getStageForRemoval();
        List<String> cwd = plainFilenamesIn(Repository.CWD);

        for (String fileName : fileNameToBlobID.keySet()) {
            String commitBlobID = fileNameToBlobID.get(fileName);
            String stagedBlobID = stageForAddition.get(fileName);
            if (!cwd.contains(fileName)) {
                // deleted in the working directory,
                // but not staged for removal or already staged for addition.
                if (!stageForRemoval.contains(fileName)
                        || stageForAddition.containsKey(fileName)) {
                    modifiedNotStagedFiles.add(fileName + " (deleted)");
                }
            } else {
                // changed in the working directory,
                // but not staged or with different contents than in the stage.
                String currentBlobID = Blob.getBlobID(fileName);
                if (stageForAddition.containsKey(fileName)) {
                    if (!stagedBlobID.equals(currentBlobID)) {
                        modifiedNotStagedFiles.add(fileName + " (modified)");
                    }
                } else {
                    if (!commitBlobID.equals(currentBlobID)) {
                        modifiedNotStagedFiles.add(fileName + " (modified)");
                    }
                }
            }
        }
        return modifiedNotStagedFiles;
    }

    // Untracked Files exist in the cwd but are not tracked by the current commit.
    public List<String> getUntrackedFiles() {
        List<String> untrackedFiles = new ArrayList<>();
        StagingArea stagingArea = StagingArea.getStagingArea();
        Map<String, String> stageForAddition = stagingArea.getStageForAddition();
        for (String fileName : plainFilenamesIn(Repository.CWD)) {
            if (!fileNameToBlobID.containsKey(fileName)
                    && !stageForAddition.containsKey(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }


    public String getParentCommitID() {
        return parentCommitID;
    }

    public String getSecondParentCommitID() {
        return secondParentCommitID;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFileNameToBlobID() {
        return fileNameToBlobID;
    }

    public String getCommitID() {
        return commitID;
    }

    public static String getInitialCommitID() {
        return initialCommitID;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===\n");
        sb.append("commit ").append(commitID).append("\n");

        if (secondParentCommitID != null) {
            sb.append("Merge: ");
            sb.append(parentCommitID, 0, 7).append(" ").append(secondParentCommitID, 0, 7);
            sb.append("\n");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        sb.append("Date: ").append(sdf.format(timeStamp)).append("\n");
        sb.append(message).append("\n");

        return sb.toString();
    }
}
