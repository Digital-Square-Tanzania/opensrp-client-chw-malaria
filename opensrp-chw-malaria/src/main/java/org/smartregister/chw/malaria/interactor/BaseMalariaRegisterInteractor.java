package org.smartregister.chw.malaria.interactor;

import androidx.annotation.VisibleForTesting;

import org.smartregister.chw.malaria.contract.MalariaRegisterContract;
import org.smartregister.chw.malaria.util.AppExecutors;
import org.smartregister.chw.malaria.util.MalariaUtil;

public class BaseMalariaRegisterInteractor implements MalariaRegisterContract.Interactor {

    private AppExecutors appExecutors;

    @VisibleForTesting
    BaseMalariaRegisterInteractor(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    public BaseMalariaRegisterInteractor() {
        this(new AppExecutors());
    }

    @Override
    public void saveRegistration(final String jsonString, final MalariaRegisterContract.InteractorCallBack callBack) {

        Runnable runnable = () -> {
            try {
                MalariaUtil.saveFormEvent(jsonString);
            } catch (Exception e) {
                e.printStackTrace();
            }

            appExecutors.mainThread().execute(() -> callBack.onRegistrationSaved());
        };
        appExecutors.diskIO().execute(runnable);
    }
}
