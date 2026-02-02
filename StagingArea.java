package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import static gitlet.Utils.*;

public class StagingArea implements Serializable {

    /** A map stores the file names along with their blobIDs */
    private Map<String, String> stageForAddition;
    /** A list stores the file names */
    private List<String> stageForRemoval;

    // If the file "stage" already exists, then read its content.
    // Otherwise, create a new "stage" file.
    private StagingArea() {
        if (Repository.GITLET_DIR.exists()) {
            File stageFile = join(Repository.GITLET_DIR, "stage");
            if (stageFile.exists()) {
                StagingArea stagingArea = readObject(stageFile, StagingArea.class);
                stageForAddition = stagingArea.stageForAddition;
                stageForRemoval = stagingArea.stageForRemoval;
                this.saveStagingArea();
            } else {
                stageForAddition = new HashMap<>();
                stageForRemoval = new ArrayList<>();
                this.saveStagingArea();
            }
        }
    }

    public static StagingArea getStagingArea() {
        return new StagingArea();
    }

    public void saveStagingArea() {
        File stageFile = join(Repository.GITLET_DIR, "stage");
        writeObject(stageFile, this);
    }

    public void addFile(File fileToBeAdded, String currentCommitID) {
        String fileName = fileToBeAdded.getName();
        if (stageForRemoval.contains(fileName)) {
            stageForRemoval.remove(fileName);
        }

        Commit currentCommit = Commit.findCommit(currentCommitID);
        Map<String, String> currentFileMap = currentCommit.getFileNameToBlobID();

        // Check if the file added is identical to any file in currentFileMap
        Blob newBlob = new Blob(readContents(fileToBeAdded));
        String newBlobID = newBlob.getBlobID();
        if (newBlobID.equals(currentFileMap.get(fileName))) {
            if (stageForAddition.containsKey(fileName)) {
                stageForAddition.remove(fileName);
            }
            return;
        }
        stageForAddition.put(fileName, newBlobID);
        newBlob.saveBlob();
    }

    public void removeFile(File fileToBeRemoved, Map<String, String> currentFileMap) {
        String fileName = fileToBeRemoved.getName();
        if (stageForAddition.containsKey(fileName)) {
            stageForAddition.remove(fileName);
        }

        if (currentFileMap.containsKey(fileName)) {
            String fileBlobID = currentFileMap.get(fileName);
            stageForRemoval.add(fileName);
            if (fileToBeRemoved.exists() && fileBlobID.equals(Blob.getBlobID(fileName))) {
                restrictedDelete(fileName);
            }
        }
    }

    public void clear() {
        stageForAddition.clear();
        stageForRemoval.clear();
    }

    public Map<String, String> getStageForAddition() {
        return stageForAddition;
    }

    public List<String> getStageForRemoval() {
        return stageForRemoval;
    }
}
