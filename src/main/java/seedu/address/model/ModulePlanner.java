package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javafx.collections.ObservableList;
import seedu.address.model.module.Module;
import seedu.address.model.module.Name;
import seedu.address.model.module.UniqueModuleList;
import seedu.address.model.semester.Semester;
import seedu.address.model.semester.SemesterName;
import seedu.address.model.studyplan.StudyPlan;
import seedu.address.model.studyplan.Title;
import seedu.address.model.studyplan.UniqueStudyPlanList;
import seedu.address.model.studyplan.exceptions.StudyPlanNotFoundException;
import seedu.address.model.tag.Tag;
import seedu.address.model.tag.UniqueTagList;
import seedu.address.model.tag.UserTag;
import seedu.address.model.versiontracking.CommitList;
import seedu.address.model.versiontracking.StudyPlanCommitManager;
import seedu.address.model.versiontracking.StudyPlanCommitManagerList;
import seedu.address.model.versiontracking.VersionTrackingManager;
import seedu.address.model.versiontracking.exception.CommitNotFoundException;
import seedu.address.model.versiontracking.exception.StudyPlanCommitManagerNotFoundException;

/**
 * Wraps all data at the module planner level
 * Duplicates are not allowed (by .isSameStudyPlan comparison)
 */
public class ModulePlanner implements ReadOnlyModulePlanner {

    private final UniqueStudyPlanList studyPlans;
    private StudyPlan activeStudyPlan;
    private final ModulesInfo modulesInfo;
    private final VersionTrackingManager versionTrackingManager;
    private SemesterName currentSemester;

    public ModulePlanner() {
        studyPlans = new UniqueStudyPlanList();
        modulesInfo = new ModulesInfo();
        versionTrackingManager = new VersionTrackingManager();
    }

    public ModulePlanner(ModulesInfo modulesInfo) {
        studyPlans = new UniqueStudyPlanList();
        this.modulesInfo = modulesInfo;
        versionTrackingManager = new VersionTrackingManager();
    }

    /**
     * Creates an ModulePlanner using the studyPlans in the {@code toBeCopied}
     */
    public ModulePlanner(ReadOnlyModulePlanner toBeCopied, ModulesInfo modulesInfo) {
        studyPlans = new UniqueStudyPlanList();
        resetData(toBeCopied);
        activeStudyPlan = toBeCopied.getActiveStudyPlan();
        this.modulesInfo = modulesInfo;
        versionTrackingManager = toBeCopied.getVersionTrackingManager();
    }

    /**
     * Creates an ModulePlanner from JSON. This is used in {@code JsonSerializableModulePlanner}.
     */
    public ModulePlanner(UniqueStudyPlanList uniqueStudyPlanList, /*StudyPlan activeStudyPlan,*/
                         ModulesInfo modulesInfo,
                         VersionTrackingManager versionTrackingManager) {
        this.studyPlans = uniqueStudyPlanList;
        // this.activeStudyPlan = activeStudyPlan;
        this.modulesInfo = modulesInfo;
        this.versionTrackingManager = versionTrackingManager;
    }

    //=========== List Overwrite Operations ================================================================

    /**
     * Replaces the contents of the person list with {@code studyPlans}.
     * {@code studyPlans} must not contain duplicate studyPlans.
     */
    public void setStudyPlans(List<StudyPlan> studyPlans) {
        this.studyPlans.setStudyPlans(studyPlans);
    }

    /**
     * Resets the existing data of this {@code ModulePlanner} with {@code newData}.
     */
    public void resetData(ReadOnlyModulePlanner newData) {
        requireNonNull(newData);

        setStudyPlans(newData.getStudyPlanList());
    }

    //=========== Study Plan-level Operations ===============================================================

    /**
     * Returns true if a study plan with the same identity as {@code study plan} exists in the module planner.
     */
    public boolean hasStudyPlan(StudyPlan studyPlan) {
        requireNonNull(studyPlan);
        return studyPlans.contains(studyPlan);
    }

    /**
     * Adds a study plan to the module planner.
     * The study plan must not already exist in the module planner.
     */
    public void addStudyPlan(StudyPlan studyPlan) {
        studyPlans.add(studyPlan);
    }

    /**
     * Replaces the given study plan {@code target} in the list with {@code editedStudyPlan}.
     * {@code target} must exist in the module planner.
     * The person identity of {@code editedStudyPlan} must not be the same as another existing study plan
     * in the module planner.
     */
    public void setStudyPlan(StudyPlan target, StudyPlan editedStudyPlan) {
        requireNonNull(editedStudyPlan);

        studyPlans.setStudyPlan(target, editedStudyPlan);
    }

    /**
     * Returns the current active {@code StudyPlan}.
     */
    public StudyPlan getActiveStudyPlan() {
        return activeStudyPlan;
    }

    /**
     * Activates the study plan with the given index, and returns the active study plan populated with relevant
     * details.
     */
    public StudyPlan activateStudyPlan(int index) throws StudyPlanNotFoundException {
        boolean foundStudyPlan = false;
        Iterator<StudyPlan> iterator = studyPlans.iterator();
        while (iterator.hasNext()) {
            StudyPlan studyPlan = iterator.next();
            if (studyPlan.getIndex() == index) {
                activeStudyPlan = studyPlan;
                foundStudyPlan = true;
            }
        }

        if (activeStudyPlan == null || !foundStudyPlan) {
            throw new StudyPlanNotFoundException();
        }

        // if this active study plan has already been activated before, then no need to activate it again.
        if (activeStudyPlan.isActivated()) {
            return activeStudyPlan;
        }

        // construct the mega list of modules
        HashMap<String, Module> megaModuleHash = activeStudyPlan.getModules();
        for (Module module : megaModuleHash.values()) {
            ModuleInfo moduleInfo = modulesInfo.find(module.getModuleCode().toString());
            module.setName(new Name(moduleInfo.getName()));
            module.setMcCount(moduleInfo.getMc());
            module.setPrereqTree(moduleInfo.getPrereqTree());

            // adds default tags to each module
            UniqueTagList defaultTags = activeStudyPlan.assignDefaultTags(moduleInfo);
            Iterator<Tag> tagIterator = defaultTags.iterator();
            while (tagIterator.hasNext()) {
                module.getTags().addTag(tagIterator.next());
            }

        }

        // replace skeletal modules under semesters with the actual reference to modules in mega list
        Iterator<Semester> semesterIterator = activeStudyPlan.getSemesters().iterator();
        while (semesterIterator.hasNext()) {
            Semester semester = semesterIterator.next();
            UniqueModuleList uniqueModuleList = semester.getModules();
            Iterator<Module> moduleIterator = uniqueModuleList.iterator();
            while (moduleIterator.hasNext()) {
                Module skeletalModule = moduleIterator.next();
                Module actualModule = megaModuleHash.get(skeletalModule.getModuleCode().toString());
                uniqueModuleList.setModule(skeletalModule, actualModule);
            }
        }

        // TODO: get user-defined tags from mega tag list, and make the tags refer to the megalist of tags
        // TODO: this is done?
        for (Module module : megaModuleHash.values()) {
            UniqueTagList tagList = module.getTags();
        }

        activeStudyPlan.updatePrereqs();

        activeStudyPlan.setActivated(true);

        return activeStudyPlan;
    }

    /**
     * Activates the first study plan in the list of study plans. This is used in {@code DeleteCommand}.
     * If there is no study plan in the list, the method returns false.
     *
     * @return boolean to indicate whether the first study plan has been activated.
     */
    public boolean activateFirstStudyPlan() {
        if (studyPlans.getSize() == 0) {
            // the active study plan will be null
            activeStudyPlan = null;
            return false;
        } else {
            int indexOfFirstStudyPlan = studyPlans.iterator().next().getIndex();
            activateStudyPlan(indexOfFirstStudyPlan);
            return true;
        }
    }

    /**
     * Removes {@code key} from this {@code ModulePlanner}.
     * {@code key} must exist in the module planner.
     */
    public void removeStudyPlan(StudyPlan key) {
        studyPlans.remove(key);
    }

    /**
     * Returns the version tracking manager of the current module planner.
     */
    public VersionTrackingManager getVersionTrackingManager() {
        return versionTrackingManager;
    }

    /**
     * Sets the current semester. The user cannot change any module before the current semester. But they can
     * still change those in the current semester and after the current semester.
     */
    public void setCurrentSemester(SemesterName semesterName) {
        currentSemester = semesterName;
        activeStudyPlan.setCurrentSemester(semesterName);
    }

    /**
     * Returns the current semester. The user cannot change any module before the current semester. But they can
     * still change those in the current semester and after the current semester.
     *
     * @return the semester name of the current semester.
     */
    public SemesterName getCurrentSemester() {
        return currentSemester;
    }

    /**
     * Returns the {@code ModuleInfo} with the given module code.
     */
    public ModuleInfo getModule(String moduleCode) {
        return modulesInfo.find(moduleCode);
    }

    /**
     * Returns module information of the given module code, as a string.
     */
    public String getModuleInformation(String moduleCode) {
        ModuleInfo moduleInfo = modulesInfo.find(moduleCode);
        return moduleInfo == null ? null : moduleInfo.getInformation();
    }

    /**
     * Deletes all the modules inside a semester of the current active study plan.
     */
    public void deleteAllModulesInSemester(SemesterName semesterName) {
        activeStudyPlan.deleteAllModulesInSemester(semesterName);
    }

    //=========== Module Information and Verification =============================================================

    /**
     * Returns this module planner's ModulesInfo object.
     */
    public ModulesInfo getModulesInfo() {
        return modulesInfo;
    }

    /**
     * Updates prerequisites of the active study plan, if it exists.
     */
    public void updatePrereqs() {
        if (this.activeStudyPlan != null) {
            this.activeStudyPlan.updatePrereqs();
        }
    }

    public List<String> getValidMods(SemesterName semName) {
        if (this.activeStudyPlan == null) {
            return new ArrayList<>(); // TODO: might want to change it to an assertion, this should not be called maybe?
        }
        return this.activeStudyPlan.getValidMods(semName);
    }

    /**
     * Changes the title of the active study plan.
     */
    public void changeActiveStudyPlanTitle(String title) {
        if (this.activeStudyPlan != null) {
            activeStudyPlan.setTitle(new Title(title));
        }
    }

    //=========== Version Tracking ============================================================================

    /**
     * Commits the current active study plan.
     */
    public void commitActiveStudyPlan(String commitMessage) {
        StudyPlanCommitManager manager = versionTrackingManager.commitStudyPlan(activeStudyPlan, commitMessage);
    }

    /**
     * Returns the commit history of a study plan with the given index.
     */
    public CommitList getCommitListByStudyPlanIndex(int index) {
        StudyPlanCommitManagerList managerList = versionTrackingManager.getStudyPlanCommitManagerList();
        StudyPlanCommitManager manager = managerList.getManagerByStudyPlanIndex(index);
        if (manager == null) {
            throw new StudyPlanCommitManagerNotFoundException();
        }
        return manager.getCommitList();
    }

    /**
     * Removes a StudyPlanCommitManager by the given StudyPlan index.
     */
    public void deleteStudyPlanCommitManagerByIndex(int index) throws StudyPlanCommitManagerNotFoundException {
        versionTrackingManager.deleteStudyPlanCommitManagerByIndex(index);
    }

    /**
     * Reverts the current active study plan to the commit specified by the given index. Make this version
     * of the study plan active.
     */
    public void revertToCommit(int studyPlanIndex, int commitNumber) {
        requireNonNull(activeStudyPlan);
        assert studyPlanIndex == activeStudyPlan.getIndex() : "The index needs to be same as the active one's";

        StudyPlanCommitManager manager = versionTrackingManager.getStudyPlanCommitManagerByStudyPlan(activeStudyPlan);
        StudyPlan newActiveStudyPlan = manager.revertToCommit(commitNumber);

        studyPlans.setStudyPlan(activeStudyPlan, newActiveStudyPlan);
        activateStudyPlan(newActiveStudyPlan.getIndex());
    }

    /**
     * Deletes the commit specified by the given study plan index and commit number.
     */
    public void deleteCommit(int studyPlanIndex, int commitNumber) throws CommitNotFoundException {
        requireNonNull(activeStudyPlan);

        if (studyPlanIndex != activeStudyPlan.getIndex()) {
            throw new StudyPlanNotFoundException();
        }

        StudyPlanCommitManager manager = versionTrackingManager.getStudyPlanCommitManagerByStudyPlan(activeStudyPlan);
        manager.deleteCommit(commitNumber);
    }

    //=========== Tagging =================================================================================

    public boolean addTagToActiveSp(UserTag tag, String moduleCode) {
        return activeStudyPlan.addTag(tag, moduleCode);
    }

    public boolean activeSpContainsTag(String tagName) {
        return activeStudyPlan.containsTag(tagName);
    }

    public Tag getTagFromActiveSp(String tagName) {
        return activeStudyPlan.getTag(tagName);
    }

    public UniqueTagList getTagsFromActiveSp() {
        return activeStudyPlan.getTags();
    }

    public UniqueTagList getModuleTagsFromActiveSp(String moduleCode) {
        return activeStudyPlan.getModuleTags(moduleCode);
    }

    public void deleteTagFromActiveSp(UserTag toDelete) {
        activeStudyPlan.deleteTag(toDelete);
    }

    public void removeTagFromAllModulesInActiveSp(UserTag toRemove) {
        activeStudyPlan.removeTagFromAllModules(toRemove);
    }

    public boolean removeTagFromModuleInActiveSp(UserTag toRemove, String moduleCode) {
        return activeStudyPlan.removeTagFromModule(toRemove, moduleCode);
    }

    public void updateAllCompletedTags() {
        activeStudyPlan.updateAllCompletedTags();
    }

    //=========== Util Methods =================================================================================
    public HashMap<String, Module> getModulesFromActiveSp() {
        return activeStudyPlan.getModules();
    }

    @Override
    public String toString() {
        return studyPlans.asUnmodifiableObservableList().size() + " studyPlans";
        // TODO: refine later
    }

    @Override
    public ObservableList<StudyPlan> getStudyPlanList() {
        return studyPlans.asUnmodifiableObservableList();
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof ModulePlanner // instanceof handles nulls
                && studyPlans.equals(((ModulePlanner) other).studyPlans));
    }

    @Override
    public int hashCode() {
        return studyPlans.hashCode();
    }
}
