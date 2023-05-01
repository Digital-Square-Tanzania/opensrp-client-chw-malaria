package org.smartregister.chw.malaria.contract;

import android.content.Context;

import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;

import java.util.LinkedHashMap;
import java.util.Map;

public interface BaseIccmVisitContract {

    interface View extends VisitView {

        BaseIccmVisitContract.Presenter presenter();

        Form getFormConfig();

        void startForm(BaseIccmVisitAction ldVisitAction);

        void startFormActivity(JSONObject jsonForm);

        void startFragment(BaseIccmVisitAction ldVisitAction);

        void redrawHeader(IccmMemberObject IccmMemberObject);

        void redrawVisitUI();

        void displayProgressBar(boolean state);

        Map<String, BaseIccmVisitAction> getLDVisitActions();

        void close();

        void submittedAndClose();

        Presenter getPresenter();

        /**
         * Save the received data into the events table
         * Start aggregation of all events and persist results into the events table
         */
        void submitVisit();

        void initializeActions(LinkedHashMap<String, BaseIccmVisitAction> map);

        Context getContext();

        void displayToast(String message);

        Boolean getEditMode();

        void onMemberDetailsReloaded(IccmMemberObject IccmMemberObject);
    }

    interface VisitView {

        /**
         * Results action when a dialog is opened and returns a payload
         *
         * @param jsonString
         */
        void onDialogOptionUpdated(String jsonString);

        Context getMyContext();
    }

    interface Presenter {

        void startForm(String formName, String memberID, String currentLocationId);

        /**
         * Recall this method to redraw ui after every submission
         *
         * @return
         */
        boolean validateStatus();

        /**
         * Preload header and visit
         */
        void initialize();

        void submitVisit();

        void reloadMemberDetails(String memberID);
    }

    interface Model {

        JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception;

    }

    interface Interactor {

        void reloadMemberDetails(String memberID, InteractorCallBack callBack);

        IccmMemberObject getMemberClient(String memberID);

        void saveRegistration(String jsonString, boolean isEditMode, final BaseIccmVisitContract.InteractorCallBack callBack);

        void calculateActions(View view, IccmMemberObject IccmMemberObject, BaseIccmVisitContract.InteractorCallBack callBack);

        void submitVisit(boolean editMode, String memberID, Map<String, BaseIccmVisitAction> map, InteractorCallBack callBack);
    }

    interface InteractorCallBack {

        void onMemberDetailsReloaded(IccmMemberObject IccmMemberObject);

        void onRegistrationSaved(boolean isEdit);

        void preloadActions(LinkedHashMap<String, BaseIccmVisitAction> map);

        void onSubmitted(boolean successful);
    }
}