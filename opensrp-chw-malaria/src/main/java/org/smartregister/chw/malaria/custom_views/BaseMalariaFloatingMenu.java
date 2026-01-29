package org.smartregister.chw.malaria.custom_views;

import android.app.Activity;
import android.content.Context;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;
import android.widget.LinearLayout;

import org.smartregister.chw.malaria.domain.MemberObject;
import org.smartregister.chw.malaria.fragment.BaseMalariaCallDialogFragment;
import org.smartregister.malaria.R;

public class BaseMalariaFloatingMenu extends LinearLayout implements View.OnClickListener {
    private MemberObject MEMBER_OBJECT;

    public BaseMalariaFloatingMenu(Context context, MemberObject MEMBER_OBJECT) {
        super(context);
        initUi();
        this.MEMBER_OBJECT = MEMBER_OBJECT;
    }

    protected void initUi() {
        inflate(getContext(), R.layout.view_malaria_floating_menu, this);
        FloatingActionButton fab = findViewById(R.id.malaria_fab);
        if (fab != null)
            fab.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.malaria_fab) {
            Activity activity = (Activity) getContext();
            BaseMalariaCallDialogFragment.launchDialog(activity, MEMBER_OBJECT);
        }  else if (view.getId() == R.id.refer_to_facility_layout) {
            Activity activity = (Activity) getContext();
            BaseMalariaCallDialogFragment.launchDialog(activity, MEMBER_OBJECT);
        }
    }
}