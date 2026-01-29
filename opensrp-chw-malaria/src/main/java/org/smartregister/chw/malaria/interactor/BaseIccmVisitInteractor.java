package org.smartregister.chw.malaria.interactor;

import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.smartregister.chw.malaria.MalariaLibrary;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.domain.Visit;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.malaria.repository.VisitRepository;
import org.smartregister.chw.malaria.util.AppExecutors;
import org.smartregister.chw.malaria.util.Constants;
import org.smartregister.chw.malaria.util.JsonFormUtils;
import org.smartregister.chw.malaria.util.NCUtils;
import org.smartregister.chw.malaria.util.VisitUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.clientandeventmodel.User;
import org.smartregister.domain.SyncStatus;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.sync.helper.ECSyncHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;


public class BaseIccmVisitInteractor implements BaseIccmVisitContract.Interactor {

    protected AppExecutors appExecutors;
    private final MalariaLibrary malariaLibrary;
    private final ECSyncHelper syncHelper;

    @VisibleForTesting
    public BaseIccmVisitInteractor(AppExecutors appExecutors, MalariaLibrary malariaLibrary, ECSyncHelper syncHelper) {
        this.appExecutors = appExecutors;
        this.malariaLibrary = malariaLibrary;
        this.syncHelper = syncHelper;

    }

    public BaseIccmVisitInteractor() {
        this(new AppExecutors(), MalariaLibrary.getInstance(), MalariaLibrary.getInstance().getEcSyncHelper());
    }

    @Override
    public void reloadMemberDetails(String memberID, BaseIccmVisitContract.InteractorCallBack callBack) {
        IccmMemberObject memberObject = getMemberClient(memberID);
        if (memberObject != null) {
            final Runnable runnable = () -> {
                appExecutors.mainThread().execute(() -> callBack.onMemberDetailsReloaded(memberObject));
            };
            appExecutors.diskIO().execute(runnable);
        }
    }

    /**
     * Override this method and return actual member object for the provided user
     *
     * @param memberID unique identifier for the user
     * @return IccmMemberObject wrapper for the user's data
     */
    @Override
    public IccmMemberObject getMemberClient(String memberID) {
        return null;
    }

    @Override
    public void saveRegistration(String jsonString, boolean isEditMode, BaseIccmVisitContract.InteractorCallBack callBack) {
        Timber.v("saveRegistration");
    }

    @Override
    public void calculateActions(final BaseIccmVisitContract.View view, IccmMemberObject memberObject, final BaseIccmVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            final LinkedHashMap<String, BaseIccmVisitAction> actionList = new LinkedHashMap<>();

            try {
                BaseIccmVisitAction ba =
                        new BaseIccmVisitAction.Builder(view.getContext(), "Sample Action")
                                .withSubtitle("")
                                .withOptional(false)
                                .withFormName("anc")
                                .build();
                actionList.put("Sample Action", ba);

            } catch (BaseIccmVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    @Override
    public void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseIccmVisitAction> map, final BaseIccmVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            boolean result = true;
            try {
                submitVisit(editMode, memberID, map, "");
            } catch (Exception e) {
                Timber.e(e);
                result = false;
            }

            final boolean finalResult = result;
            appExecutors.mainThread().execute(() -> callBack.onSubmitted(finalResult));
        };

        appExecutors.diskIO().execute(runnable);
    }

    protected void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseIccmVisitAction> map, String parentEventType) throws Exception {
        // create a map of the different types

        Map<String, BaseIccmVisitAction> externalVisits = new HashMap<>();
        Map<String, String> combinedJsons = new HashMap<>();
        String payloadType = null;
        String payloadDetails = null;

        // aggregate forms to be processed
        for (Map.Entry<String, BaseIccmVisitAction> entry : map.entrySet()) {
            String json = entry.getValue().getJsonPayload();
            if (StringUtils.isNotBlank(json)) {
                // do not process events that are meant to be in detached mode
                // in a similar manner to the the aggregated events

                BaseIccmVisitAction action = entry.getValue();
                BaseIccmVisitAction.ProcessingMode mode = action.getProcessingMode();

                if (mode == BaseIccmVisitAction.ProcessingMode.SEPARATE && StringUtils.isBlank(parentEventType)) {
                    externalVisits.put(entry.getKey(), entry.getValue());
                } else {
                    combinedJsons.put(entry.getKey(), json);
                }

                payloadType = action.getPayloadType().name();
                payloadDetails = action.getPayloadDetails();
            }
        }

        String type = StringUtils.isBlank(parentEventType) ? getEncounterType() : getEncounterType();

        // persist to database
        Visit visit = saveVisit(editMode, memberID, type, combinedJsons, parentEventType);
        if (visit != null) {
            saveVisitDetails(visit, payloadType, payloadDetails);
            processExternalVisits(visit, externalVisits, memberID);
        }

        if (malariaLibrary.isSubmitOnSave()) {
            List<Visit> visits = new ArrayList<>(1);
            visits.add(visit);
            VisitUtils.processVisits(visits, malariaLibrary.visitRepository(), malariaLibrary.visitDetailsRepository());
        }
    }

    /**
     * recursively persist visits to the db
     *
     * @param visit
     * @param externalVisits
     * @param memberID
     * @throws Exception
     */
    protected void processExternalVisits(Visit visit, Map<String, BaseIccmVisitAction> externalVisits, String memberID) throws Exception {
        if (visit != null && !externalVisits.isEmpty()) {
            for (Map.Entry<String, BaseIccmVisitAction> entry : externalVisits.entrySet()) {
                Map<String, BaseIccmVisitAction> subEvent = new HashMap<>();
                subEvent.put(entry.getKey(), entry.getValue());

                String subMemberID = entry.getValue().getBaseEntityID();
                if (StringUtils.isBlank(subMemberID))
                    subMemberID = memberID;

                submitVisit(false, subMemberID, subEvent, visit.getVisitType());
            }
        }
    }

    protected @Nullable Visit saveVisit(boolean editMode, String memberID, String encounterType,
                                        final Map<String, String> jsonString,
                                        String parentEventType
    ) throws Exception {

        AllSharedPreferences allSharedPreferences = MalariaLibrary.getInstance().context().allSharedPreferences();

        String derivedEncounterType = StringUtils.isBlank(parentEventType) ? encounterType : "";
        Event baseEvent = JsonFormUtils.processVisitJsonForm(allSharedPreferences, memberID, derivedEncounterType, jsonString, getTableName());

        // only tag the first event with the date
        if (StringUtils.isBlank(parentEventType)) {
            prepareEvent(baseEvent);
        } else {
            prepareSubEvent(baseEvent);
        }

        if (baseEvent != null) {
            baseEvent.setFormSubmissionId(JsonFormUtils.generateRandomUUIDString());
            JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);

            String visitID = (editMode) ?
                    visitRepository().getLatestVisit(memberID, getEncounterType()).getVisitId() :
                    JsonFormUtils.generateRandomUUIDString();

            // reset database
            if (editMode) {
                deleteProcessedVisit(visitID, memberID);
                deleteOldVisit(visitID);
            }

            Visit visit = NCUtils.eventToVisit(baseEvent, visitID);
            visit.setPreProcessedJson(new Gson().toJson(baseEvent));
            visit.setParentVisitID(getParentVisitEventID(visit, parentEventType));

            visitRepository().addVisit(visit);
            return visit;
        }
        return null;
    }

    protected String getParentVisitEventID(Visit visit, String parentEventType) {
        return visitRepository().getParentVisitEventID(visit.getBaseEntityId(), parentEventType, visit.getDate());
    }

    @VisibleForTesting
    public VisitRepository visitRepository() {
        return MalariaLibrary.getInstance().visitRepository();
    }

    protected void deleteOldVisit(String visitID) {
        visitRepository().deleteVisit(visitID);
        MalariaLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(visitID);

        List<Visit> childVisits = visitRepository().getChildEvents(visitID);
        for (Visit v : childVisits) {
            visitRepository().deleteVisit(v.getVisitId());
            MalariaLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(v.getVisitId());
        }
    }


    protected void deleteProcessedVisit(String visitID, String baseEntityId) {
        // check if the event
        AllSharedPreferences allSharedPreferences = MalariaLibrary.getInstance().context().allSharedPreferences();
        Visit visit = visitRepository().getVisitByVisitId(visitID);
        if (visit == null || !visit.getProcessed()) return;

        Event processedEvent = IccmDao.getEventByFormSubmissionId(visit.getFormSubmissionId());
        if (processedEvent == null) return;

        deleteSavedEvent(allSharedPreferences, baseEntityId, processedEvent.getEventId(), processedEvent.getFormSubmissionId(), "event");
    }

    protected void deleteSavedEvent(AllSharedPreferences allSharedPreferences, String baseEntityId, String eventId, String formSubmissionId, String type) {
        Event event = (Event) new Event()
                .withBaseEntityId(baseEntityId)
                .withEventDate(new Date())
                .withEventType(Constants.EVENT_TYPE.VOID_EVENT)
                .withLocationId(JsonFormUtils.locationId(allSharedPreferences))
                .withProviderId(allSharedPreferences.fetchRegisteredANM())
                .withEntityType(type)
                .withFormSubmissionId(formSubmissionId)
                .withVoided(true)
                .withVoider(new User(null, allSharedPreferences.fetchRegisteredANM(), null, null))
                .withVoidReason("Edited Event")
                .withDateVoided(new Date());

        event.setSyncStatus(SyncStatus.PENDING.value());

        try {
            syncHelper.addEvent(event.getBaseEntityId(), new JSONObject(JsonFormUtils.gson.toJson(event)));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void saveVisitDetails(Visit visit, String payloadType, String payloadDetails) {
        if (visit.getVisitDetails() == null) return;

        for (Map.Entry<String, List<VisitDetail>> entry : visit.getVisitDetails().entrySet()) {
            if (entry.getValue() != null) {
                for (VisitDetail d : entry.getValue()) {
                    d.setPreProcessedJson(payloadDetails);
                    d.setPreProcessedType(payloadType);
                    MalariaLibrary.getInstance().visitDetailsRepository().addVisitDetails(d);
                }
            }
        }
    }

    /**
     * Injects implementation specific changes to the event
     *
     * @param baseEvent
     */
    protected void prepareEvent(Event baseEvent) {
        if (baseEvent != null) {
            // add iccm date obs and last
            List<Object> list = new ArrayList<>();
            list.add(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
            baseEvent.addObs(new Obs("concept", "text", "iccm_visit_date", "",
                    list, new ArrayList<>(), null, "iccm_visit_date"));
        }
    }

    /**
     * injects additional meta data to the event
     *
     * @param baseEvent
     */
    protected void prepareSubEvent(Event baseEvent) {
        Timber.v("You can add information to sub events");
    }

    protected String getEncounterType() {
        return Constants.EVENT_TYPE.ICCM_SERVICES_VISIT;
    }

    protected String getTableName() {
        return Constants.TABLES.ICCM_ENROLLMENT;
    }

}