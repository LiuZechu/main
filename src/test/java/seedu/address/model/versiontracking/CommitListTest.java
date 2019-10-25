package seedu.address.model.versiontracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.TypicalVersionTrackingManager;

public class CommitListTest {

    @Test
    public void getCommitByIndex_validName_returnsExpectedCommit() {
        CommitList testCommitList = TypicalVersionTrackingManager.getTypicalCommitList();
        assertEquals(testCommitList.getCommitByIndex(0), TypicalVersionTrackingManager.getTypicalCommit(1));
    }

}
