package org.smartregister.chw.malaria.util;


import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.malaria.MalariaLibrary;
import org.smartregister.chw.malaria.domain.Visit;
import org.smartregister.chw.malaria.domain.VisitDetail;
import org.smartregister.chw.malaria.repository.VisitDetailsRepository;
import org.smartregister.chw.malaria.repository.VisitRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VisitUtils {
    public static List<Visit> getVisits(String memberID, String... eventTypes) {

        List<Visit> visits = (eventTypes != null && eventTypes.length > 0) ? getVisitsOnly(memberID, eventTypes[0]) : getVisitsOnly(memberID, Constants.EVENT_TYPE.ICCM_SERVICES_VISIT);

        return visits;
    }

    public static List<Visit> getVisitsOnly(String memberID, String visitName) {
        return new ArrayList<>(MalariaLibrary.getInstance().visitRepository().getVisits(memberID, visitName));
    }

    public static List<VisitDetail> getVisitDetailsOnly(String visitID) {
        return MalariaLibrary.getInstance().visitDetailsRepository().getVisits(visitID);
    }

    public static Map<String, List<VisitDetail>> getVisitGroups(List<VisitDetail> detailList) {
        Map<String, List<VisitDetail>> visitMap = new HashMap<>();

        for (VisitDetail visitDetail : detailList) {

            List<VisitDetail> visitDetailList = visitMap.get(visitDetail.getVisitKey());
            if (visitDetailList == null)
                visitDetailList = new ArrayList<>();

            visitDetailList.add(visitDetail);

            visitMap.put(visitDetail.getVisitKey(), visitDetailList);
        }
        return visitMap;
    }

    public static void processVisits(List<Visit> visits, VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository) throws Exception {
        String visitGroupId = UUID.randomUUID().toString();
        for (Visit v : visits) {
            if (!v.getProcessed()) {

                // persist to db
                Event baseEvent = new Gson().fromJson(v.getPreProcessedJson(), Event.class);
                if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                    baseEvent.setFormSubmissionId(UUID.randomUUID().toString());

                baseEvent.addDetails(Constants.ICCM_VISIT_GROUP, visitGroupId);

                AllSharedPreferences allSharedPreferences = MalariaLibrary.getInstance().context().allSharedPreferences();
                NCUtils.addEvent(allSharedPreferences, baseEvent);

                // process details
                //   processVisitDetails(visitGroupId, v, visitDetailsRepository, v.getVisitId(), v.getBaseEntityId(), baseEvent.getFormSubmissionId());

                visitRepository.completeProcessing(v.getVisitId());
            }
        }

        // process after all events are saved
        NCUtils.startClientProcessing();

    }

}
