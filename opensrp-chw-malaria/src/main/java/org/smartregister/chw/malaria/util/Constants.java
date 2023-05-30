package org.smartregister.chw.malaria.util;

public interface Constants {

    int REQUEST_CODE_GET_JSON = 2244;
    String ENCOUNTER_TYPE = "encounter_type";
    String STEP_ONE = "step1";
    String STEP_TWO = "step2";
    String STEP_THREE = "step3";
    String STEP_FOUR = "step4";
    String ICCM_VISIT_GROUP = "iccm_visit_group";
    interface JSON_FORM_EXTRA {
        String JSON = "json";
        String ENCOUNTER_TYPE = "encounter_type";
    }

    interface EVENT_TYPE {
        String MALARIA_CONFIRMATION = "Malaria Confirmation";
        String MALARIA_FOLLOW_UP_VISIT = "Malaria Follow-up Visit";
        String ICCM_ENROLLMENT = "ICCM Enrollment";
        String ICCM_SERVICES_VISIT = "ICCM Services Visit";
        String VOID_EVENT = "Void Event";
    }

    interface FORMS {
        String MALARIA_REGISTRATION = "malaria_confirmation";
        String MALARIA_FOLLOW_UP_VISIT = "malaria_followup_visit";
    }

    interface TABLES {
        String MALARIA_CONFIRMATION = "ec_malaria_confirmation";
        String MALARIA_FOLLOW_UP = "ec_malaria_follow_up_visit";
        String ICCM_ENROLLMENT = "ec_iccm_enrollment";
    }

    interface ACTIVITY_PAYLOAD {
        String BASE_ENTITY_ID = "BASE_ENTITY_ID";
        String FAMILY_BASE_ENTITY_ID = "FAMILY_BASE_ENTITY_ID";
        String ACTION = "ACTION";
        String MALARIA_FORM_NAME = "MALARIA_FORM_NAME";
        String EDIT_MODE = "editMode";
        String MEMBER_PROFILE_OBJECT = "MemberObject";

    }

    interface ACTIVITY_PAYLOAD_TYPE {
        String REGISTRATION = "REGISTRATION";
        String FOLLOW_UP_VISIT = "FOLLOW_UP_VISIT";
    }

    interface CONFIGURATION {
        String MALARIA_CONFIRMATION = "malaria_confirmation";
    }

    interface MALARIA_MEMBER_OBJECT {
        String MEMBER_OBJECT = "memberObject";
    }

}