package org.smartregister.chw.malaria.presenter;

import org.json.JSONObject;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.malaria.util.JsonFormUtils;
import org.smartregister.malaria.R;
import org.smartregister.util.FormUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

import timber.log.Timber;

public class BaseIccmVisitPresenter implements BaseIccmVisitContract.Presenter, BaseIccmVisitContract.InteractorCallBack {

    protected WeakReference<BaseIccmVisitContract.View> view;
    protected BaseIccmVisitContract.Interactor interactor;
    protected IccmMemberObject memberObject;

    public BaseIccmVisitPresenter(IccmMemberObject memberObject, BaseIccmVisitContract.View view, BaseIccmVisitContract.Interactor interactor) {
        this.view = new WeakReference<>(view);
        this.interactor = interactor;
        this.memberObject = memberObject;
    }

    @Override
    public void startForm(String formName, String memberID, String currentLocationId) {
        try {
            if (view.get() != null) {
                JSONObject jsonObject = FormUtils.getInstance(view.get().getContext()).getFormJson(formName);
                JsonFormUtils.getRegistrationForm(jsonObject, memberID, currentLocationId);
                view.get().startFormActivity(jsonObject);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public boolean validateStatus() {
        return false;
    }

    @Override
    public void initialize() {
        view.get().displayProgressBar(true);
        view.get().redrawHeader(memberObject);
        interactor.calculateActions(view.get(), memberObject, this);
    }

    @Override
    public void submitVisit() {
        if (view.get() != null) {
            view.get().displayProgressBar(true);
            interactor.submitVisit(view.get().getEditMode(), memberObject.getBaseEntityId(), view.get().getLDVisitActions(), this);
        }
    }

    @Override
    public void reloadMemberDetails(String memberID) {
        view.get().displayProgressBar(true);
        interactor.reloadMemberDetails(memberID, this);
    }

    @Override
    public void onMemberDetailsReloaded(IccmMemberObject memberObject) {
        if (view.get() != null) {
            this.memberObject = memberObject;

            view.get().displayProgressBar(false);
            view.get().onMemberDetailsReloaded(memberObject);
        }
    }

    @Override
    public void onRegistrationSaved(boolean isEdit) {
        Timber.v("onRegistrationSaved");
    }

    @Override
    public void preloadActions(LinkedHashMap<String, BaseIccmVisitAction> map) {
        if (view.get() != null)
            view.get().initializeActions(map);
    }

    @Override
    public void onSubmitted(boolean successful) {
        if (view.get() != null) {
            view.get().displayProgressBar(false);
            if (successful) {
                view.get().submittedAndClose();
            } else {
                view.get().displayToast(view.get().getContext().getString(R.string.error_unable_save_visit));
            }
        }
    }
}
