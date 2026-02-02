package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.Map;

import static gitlet.Utils.*;

/**
 * Stores remote repository mappings.
 * The mappings are saved in .gitlet/remotes file as a serialized TreeMap.
 */
public class Remote implements Serializable {

    /** File that stores all remote mappings. */
    private static final File REMOTES_FILE = Utils.join(Repository.REMOTES_DIR, "remotes");
    /** Map of remote name -> path. */
    private TreeMap<String, String> remoteMap;

    @SuppressWarnings("unchecked")
    private Remote() {
        if (!Repository.REMOTES_DIR.exists()) {
            Repository.REMOTES_DIR.mkdir();
        }
        if (REMOTES_FILE.exists()) {
            remoteMap = (TreeMap<String, String>) readObject(REMOTES_FILE, TreeMap.class);
        } else {
            remoteMap = new TreeMap<>();
        }
    }

    // Load remotes from file, or initialize an empty map.
    public static Remote loadRemotes() {
        return new Remote();
    }

    // Save current remotes to file.
    private void saveRemotes() {
        writeObject(REMOTES_FILE, (Serializable) remoteMap);
    }

     // Add a remote with given name and path.
    public void addRemote(String name, String path) {
        if (remoteMap.containsKey(name)) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        path = normalizePath(path);
        remoteMap.put(name, path);
        saveRemotes();
    }

     // Remove a remote with given name.
    public void removeRemote(String name) {
        if (!remoteMap.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        remoteMap.remove(name);
        saveRemotes();
    }

    public String getRemotePath(String name) {
        return remoteMap.get(name);
    }

    public Map<String, String> getRemoteMap() {
        return remoteMap;
    }

     // Normalize the path to match system-specific separator.
    private String normalizePath(String inputPath) {
        return inputPath.replace("/", File.separator);
    }

    public static void copyCommitsFromRemote(String commitID, File remoteGitletDir) {
        if (commitID == null) {
            return;
        }
        File localCommitFile = join(Repository.COMMITS_DIR, commitID);
        if (localCommitFile.exists()) {
            return;
        }

        File remoteCommitFile = join(remoteGitletDir, "commits", commitID);
        writeContents(localCommitFile, readContents(remoteCommitFile));
        Commit remoteCommit = readObject(remoteCommitFile, Commit.class);
        Map<String, String> remoteCommitFiles = remoteCommit.getFileNameToBlobID();
        for (String blobID : remoteCommitFiles.values()) {
            File localBlobFile = join(Repository.BLOBS_DIR, blobID);
            if (!localBlobFile.exists()) {
                File remoteBlobFile = join(remoteGitletDir, "blobs", blobID);
                writeContents(localBlobFile, readContents(remoteBlobFile));
            }
        }

        copyCommitsFromRemote(remoteCommit.getParentCommitID(), remoteGitletDir);
        String secondParentID = remoteCommit.getSecondParentCommitID();
        if (secondParentID != null) {
            copyCommitsFromRemote(secondParentID, remoteGitletDir);
        }
    }

    public static void copyCommitsToRemote(String commitID, File remoteGitletDir) {
        File remoteCommitFile = join(remoteGitletDir, "commits", commitID);
        if (remoteCommitFile.exists()) {
            return;
        }

        File localCommitFile = join(Repository.COMMITS_DIR, commitID);
        writeContents(remoteCommitFile, readContents(localCommitFile));
        Commit localCommit = readObject(localCommitFile, Commit.class);
        Map<String, String> fileMap = localCommit.getFileNameToBlobID();
        for (String blobID : fileMap.values()) {
            File remoteBlobFile = join(remoteGitletDir, "blobs", blobID);
            if (!remoteBlobFile.exists()) {
                File localBlobFile = join(Repository.BLOBS_DIR, blobID);
                writeContents(remoteBlobFile, readContents(localBlobFile));
            }
        }

        copyCommitsToRemote(localCommit.getParentCommitID(), remoteGitletDir);
        String secondParentID = localCommit.getSecondParentCommitID();
        if (secondParentID != null) {
            copyCommitsToRemote(secondParentID, remoteGitletDir);
        }
    }
}
