package org.smartregister.chw.malaria.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.smartregister.chw.malaria.contract.BaseIccmVisitContract;
import org.smartregister.chw.malaria.model.BaseIccmVisitAction;
import org.smartregister.malaria.R;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class BaseIccmVisitAdapter extends RecyclerView.Adapter<BaseIccmVisitAdapter.MyViewHolder> {
    private final Map<String, BaseIccmVisitAction> iccmHomeVisitActionList;
    private final Context context;
    private final BaseIccmVisitContract.View visitContractView;

    public BaseIccmVisitAdapter(Context context, BaseIccmVisitContract.View view, LinkedHashMap<String, BaseIccmVisitAction> myDataset) {
        iccmHomeVisitActionList = myDataset;
        this.context = context;
        this.visitContractView = view;
    }

    @NotNull
    @Override
    public BaseIccmVisitAdapter.MyViewHolder onCreateViewHolder(@NotNull ViewGroup parent,
                                                                int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.iccm_visit_item, parent, false);
        return new MyViewHolder(v);
    }

    /**
     * get the position of the the valid items in the data set
     *
     * @param position
     * @return
     */
    private BaseIccmVisitAction getByPosition(int position) {
        int count = -1;
        for (Map.Entry<String, BaseIccmVisitAction> entry : iccmHomeVisitActionList.entrySet()) {
            if (entry.getValue().isValid())
                count++;

            if (count == position)
                return entry.getValue();
        }

        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NotNull MyViewHolder holder, int position) {

        BaseIccmVisitAction ldVisitAction = getByPosition(position);
        if (ldVisitAction == null)

            return;

        if (!ldVisitAction.isEnabled()) {
            holder.titleText.setTextColor(context.getResources().getColor(R.color.grey));
            holder.descriptionText.setTextColor(context.getResources().getColor(R.color.grey));
        } else {
            holder.titleText.setTextColor(context.getResources().getColor(R.color.black));
        }

        String title = MessageFormat.format("{0}<i>{1}</i>",
                ldVisitAction.getTitle(),
                ldVisitAction.isOptional() ? "" : "*"
        );
        holder.titleText.setText(Html.fromHtml(title));
        if (StringUtils.isNotBlank(ldVisitAction.getSubTitle())) {

            if (ldVisitAction.isEnabled()) {
                holder.descriptionText.setVisibility(View.VISIBLE);
                holder.invalidText.setVisibility(View.GONE);
                holder.descriptionText.setText(ldVisitAction.getSubTitle());

                boolean isOverdue = ldVisitAction.getScheduleStatus() == BaseIccmVisitAction.ScheduleStatus.OVERDUE &&
                        ldVisitAction.isEnabled();

                holder.descriptionText.setTextColor(
                        isOverdue ? context.getResources().getColor(R.color.alert_urgent_red) :
                                context.getResources().getColor(android.R.color.darker_gray)
                );

            } else {
                holder.descriptionText.setVisibility(View.GONE);
                holder.invalidText.setVisibility(View.VISIBLE);
                holder.invalidText.setText(Html.fromHtml("<i>" + ldVisitAction.getDisabledMessage() + "</i>"));
            }
        } else {
            holder.descriptionText.setVisibility(View.GONE);
        }

        int color_res = getCircleColor(ldVisitAction);

        holder.circleImageView.setCircleBackgroundColor(context.getResources().getColor(color_res));
        holder.circleImageView.setImageResource(R.drawable.ic_checked);
        holder.circleImageView.setColorFilter(context.getResources().getColor(R.color.white));

        if (color_res == R.color.transparent_gray) {
            holder.circleImageView.setBorderColor(context.getResources().getColor(R.color.light_grey));
        } else {
            holder.circleImageView.setBorderColor(context.getResources().getColor(color_res));
        }

        bindClickListener(holder.getView(), ldVisitAction);
    }

    private int getCircleColor(BaseIccmVisitAction ldVisitAction) {

        int color_res;
        boolean valid = ldVisitAction.isValid() && ldVisitAction.isEnabled();
        if (!valid)
            return R.color.transparent_gray;

        switch (ldVisitAction.getActionStatus()) {
            case PENDING:
                color_res = R.color.transparent_gray;
                break;
            case COMPLETED:
                color_res = R.color.alert_complete_green;
                break;
            case PARTIALLY_COMPLETED:
                color_res = R.color.pnc_circle_yellow;
                break;
            default:
                color_res = R.color.alert_complete_green;
                break;
        }
        return color_res;
    }

    private void bindClickListener(View view, final BaseIccmVisitAction ldVisitAction) {
        if (!ldVisitAction.isEnabled() || !ldVisitAction.isValid()) {
            view.setOnClickListener(null);
            return;
        }

        view.setOnClickListener(v -> {
            if (StringUtils.isNotBlank(ldVisitAction.getFormName())) {
                visitContractView.startForm(ldVisitAction);
            } else {
                visitContractView.startFragment(ldVisitAction);
            }
            visitContractView.redrawVisitUI();
        });
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (Map.Entry<String, BaseIccmVisitAction> entry : iccmHomeVisitActionList.entrySet()) {
            if (entry.getValue().isValid())
                count++;
        }

        return count;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView invalidText;
        private final TextView descriptionText;
        private final CircleImageView circleImageView;
        private final LinearLayout myView;

        private MyViewHolder(View view) {
            super(view);
            titleText = view.findViewById(R.id.customFontTextViewTitle);
            descriptionText = view.findViewById(R.id.customFontTextViewDetails);
            invalidText = view.findViewById(R.id.customFontTextViewInvalid);
            circleImageView = view.findViewById(R.id.circleImageView);
            myView = view.findViewById(R.id.linearLayoutHomeVisitItem);
        }

        public View getView() {
            return myView;
        }
    }

}
