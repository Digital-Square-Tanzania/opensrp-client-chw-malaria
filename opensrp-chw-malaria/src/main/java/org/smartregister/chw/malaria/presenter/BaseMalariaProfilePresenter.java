package org.smartregister.chw.malaria.presenter;

import android.content.Context;
import androidx.annotation.Nullable;

import org.smartregister.chw.malaria.contract.MalariaProfileContract;
import org.smartregister.chw.malaria.domain.MemberObject;

import java.lang.ref.WeakReference;

import timber.log.Timber;


public class BaseMalariaProfilePresenter implements MalariaProfileContract.Presenter {
    protected WeakReference<MalariaProfileContract.View> view;
    protected MemberObject memberObject;
    protected MalariaProfileContract.Interactor interactor;
    protected Context context;

    public BaseMalariaProfilePresenter(MalariaProfileContract.View view, MalariaProfileContract.Interactor interactor, MemberObject memberObject) {
        this.view = new WeakReference<>(view);
        this.memberObject = memberObject;
        this.interactor = interactor;
    }

    @Override
    public void fillProfileData(MemberObject memberObject) {
        if (memberObject != null && getView() != null) {
            getView().setProfileViewWithData();
        }
    }

    @Override
    public void recordMalariaButton(@Nullable String visitState) {
        if (getView() == null) {
            return;
        }

        if (("OVERDUE").equals(visitState) || ("DUE").equals(visitState)) {
            if (("OVERDUE").equals(visitState)) {
                getView().setOverDueColor();
            }
        } else {
            getView().hideView();
        }
    }

    @Override
    @Nullable
    public MalariaProfileContract.View getView() {
        if (view != null && view.get() != null)
            return view.get();

        return null;
    }

    @Override
    public void refreshProfileBottom() {
        interactor.refreshProfileInfo(memberObject, getView());
    }

    @Override
    public void saveForm(String jsonString) {
        try {
            interactor.saveRegistration(jsonString, getView());
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
