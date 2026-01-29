package org.smartregister.chw.malaria.activity;


import static org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.EDIT_MODE;
import static org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.FORM_SUBMISSION_ID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.chw.malaria.MalariaLibrary;
import org.smartregister.chw.malaria.adapter.BaseIccmVisitAdapter;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
import org.smartregister.chw.malaria.dao.IccmDao;
import org.smartregister.chw.malaria.domain.IccmMemberObject;
import org.smartregister.chw.malaria.interactor.BaseIccmVisitInteractor;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.chw.malaria.presenter.BaseIccmVisitPresenter;
import org.smartregister.chw.malaria.util.Constants;
import org.smartregister.malaria.R;
import org.smartregister.view.activity.SecuredActivity;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

public class BaseIccmVisitActivity extends SecuredActivity implements BaseIccmVisitContract.View, View.OnClickListener {
    protected Map<String, BaseIccmVisitAction> actionList = new LinkedHashMap<>();
    protected BaseIccmVisitContract.Presenter presenter;
    protected IccmMemberObject memberObject;
    protected String formSubmissionId;
    protected Boolean isEditMode = false;
    protected RecyclerView.Adapter mAdapter;
    protected ProgressBar progressBar;
    protected TextView tvSubmit;
    protected TextView tvTitle;
    protected String current_action;
    protected String confirmCloseTitle;
    protected String confirmCloseMessage;

    public static void startMe(Activity activity, String formSubmissionId, Boolean isEditMode) {
        Intent intent = new Intent(activity, BaseIccmVisitActivity.class);
        intent.putExtra(FORM_SUBMISSION_ID, formSubmissionId);
        intent.putExtra(EDIT_MODE, isEditMode);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_iccm_visit);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isEditMode = getIntent().getBooleanExtra(EDIT_MODE, false);
            formSubmissionId = getIntent().getStringExtra(FORM_SUBMISSION_ID);
            memberObject = IccmDao.getMember(formSubmissionId);
        }

        confirmCloseTitle = getString(R.string.confirm_form_close);
        confirmCloseMessage = getString(R.string.confirm_form_close_explanation);
        setUpView();
        displayProgressBar(true);
        registerPresenter();
        if (presenter != null) {
            if (StringUtils.isNotBlank(formSubmissionId)) {
                presenter.reloadMemberDetails(formSubmissionId);
            } else {
                presenter.initialize();
            }
        }
    }

    public void setUpView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(false);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        findViewById(R.id.close).setOnClickListener(this);
        tvSubmit = findViewById(R.id.customFontTextViewSubmit);
        tvSubmit.setOnClickListener(this);
        tvTitle = findViewById(R.id.customFontTextViewName);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new BaseIccmVisitAdapter(this, this, (LinkedHashMap) actionList);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        redrawVisitUI();
    }

    protected void registerPresenter() {
        presenter = new BaseIccmVisitPresenter(memberObject, this, new BaseIccmVisitInteractor());
    }

    @Override
    public void initializeActions(LinkedHashMap<String, BaseIccmVisitAction> map) {
        for (Map.Entry<String, BaseIccmVisitAction> entry : map.entrySet()) {
            actionList.put(entry.getKey(), entry.getValue());
        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        displayProgressBar(false);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void displayToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public Boolean getEditMode() {
        return isEditMode;
    }

    @Override
    public void onMemberDetailsReloaded(IccmMemberObject memberObject) {
        this.memberObject = memberObject;
        presenter.initialize();
        redrawHeader(memberObject);
    }

    @Override
    protected void onCreation() {
        Timber.v("Empty onCreation");
    }

    @Override
    protected void onResumption() {
        Timber.v("Empty onResumption");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.close) {
            displayExitDialog(() -> close());
        } else if (v.getId() == R.id.customFontTextViewSubmit) {
            submitVisit();
        }
    }

    @Override
    public BaseIccmVisitContract.Presenter presenter() {
        return presenter;
    }

    @Override
    public Form getFormConfig() {
        return null;
    }

    @Override
    public void startForm(BaseIccmVisitAction ldVisitAction) {
        current_action = ldVisitAction.getTitle();

        if (StringUtils.isNotBlank(ldVisitAction.getJsonPayload())) {
            try {
                JSONObject jsonObject = new JSONObject(ldVisitAction.getJsonPayload());
                startFormActivity(jsonObject);
            } catch (Exception e) {
                Timber.e(e);
                String locationId = MalariaLibrary.getInstance().context().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
                presenter().startForm(ldVisitAction.getFormName(), memberObject.getBaseEntityId(), locationId);
            }
        } else {
            String locationId = MalariaLibrary.getInstance().context().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            presenter().startForm(ldVisitAction.getFormName(), memberObject.getBaseEntityId(), locationId);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, JsonFormActivity.class);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }

        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void startFragment(BaseIccmVisitAction iccmVisitAction) {
        current_action = iccmVisitAction.getTitle();

        if (iccmVisitAction.getDestinationFragment() != null)
            iccmVisitAction.getDestinationFragment().show(getSupportFragmentManager(), current_action);

    }

    @Override
    public void redrawHeader(IccmMemberObject memberObject) {
        tvTitle.setText(MessageFormat.format("{0}, {1} \u00B7 {2}", memberObject.getFullName(), memberObject.getAge(), getString(R.string.iccm_visit)));
    }

    @Override
    public void redrawVisitUI() {
        boolean valid = actionList.size() > 0;
        for (Map.Entry<String, BaseIccmVisitAction> entry : actionList.entrySet()) {
            BaseIccmVisitAction action = entry.getValue();
            if (
                    (!action.isOptional() && (action.getActionStatus() == BaseIccmVisitAction.Status.PENDING && action.isValid()))
                            || !action.isEnabled()
            ) {
                valid = false;
                break;
            }
        }

        int res_color = valid ? R.color.white : R.color.light_grey;
        tvSubmit.setTextColor(getResources().getColor(res_color));
        tvSubmit.setOnClickListener(valid ? this : null); // update listener to null

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void displayProgressBar(boolean state) {
        progressBar.setVisibility(state ? View.VISIBLE : View.GONE);
    }


    @Override
    public Map<String, BaseIccmVisitAction> getLDVisitActions() {
        return actionList;
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void submittedAndClose() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        close();
    }

    @Override
    public BaseIccmVisitContract.Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void submitVisit() {
        getPresenter().submitVisit();
    }

    @Override
    public void onDialogOptionUpdated(String jsonString) {
        BaseIccmVisitAction ldVisitAction = actionList.get(current_action);
        if (ldVisitAction != null) {
            ldVisitAction.setJsonPayload(jsonString);
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            redrawVisitUI();
        }
    }

    @Override
    public Context getMyContext() {
        return this;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_GET_JSON) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
                    BaseIccmVisitAction ldVisitAction = actionList.get(current_action);
                    if (ldVisitAction != null) {
                        ldVisitAction.setJsonPayload(jsonString);
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            } else {

                BaseIccmVisitAction ldVisitAction = actionList.get(current_action);
                if (ldVisitAction != null)
                    ldVisitAction.evaluateStatus();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        // update the adapter after every payload
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            redrawVisitUI();
        }
    }

    @Override
    public void onBackPressed() {
        displayExitDialog(BaseIccmVisitActivity.this::finish);
    }

    protected void displayExitDialog(final Runnable onConfirm) {
        AlertDialog dialog = new AlertDialog.Builder(this, com.vijay.jsonwizard.R.style.AppThemeAlertDialog).setTitle(confirmCloseTitle)
                .setMessage(confirmCloseMessage).setNegativeButton(com.vijay.jsonwizard.R.string.yes, (dialog1, which) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                }).setPositiveButton(com.vijay.jsonwizard.R.string.no, (dialog2, which) -> Timber.d("No button on dialog in %s", JsonFormActivity.class.getCanonicalName())).create();

        dialog.show();
    }

}