package seedu.address.model.versiontracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.address.model.studyplan.StudyPlan;
import seedu.address.testutil.StudyPlanBuilder;

public class CommitTest {

    public static final String COMMIT_MESSAGE = "test commit message";

    private static final StudyPlan testStudyPlan = new StudyPlanBuilder().build();

    private Commit testCommit = new Commit(testStudyPlan, COMMIT_MESSAGE);

    @Test
    public void getCommitMessage() {
        assertEquals(testCommit.getCommitMessage(), COMMIT_MESSAGE);
    }

    @Test
    public void getStudyPlan() {
        assertEquals(testCommit.getStudyPlan(), testStudyPlan);
    }
}
