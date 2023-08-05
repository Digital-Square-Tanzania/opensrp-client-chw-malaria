package org.smartregister.chw.malaria.domain;

import org.smartregister.util.Utils;

import java.io.Serializable;
import java.util.Date;

public class IccmMemberObject extends MemberObject implements Serializable {
    private Double temperature;
    private Integer respiratoryRate;
    private Double weight;
    private String iccmEnrollmentFormSubmissionId;

    public IccmMemberObject() {
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getFullName() {
        return Utils.getName(getFirstName(), getLastName());
    }

    public String getIccmEnrollmentFormSubmissionId() {
        return iccmEnrollmentFormSubmissionId;
    }

    public void setIccmEnrollmentFormSubmissionId(String iccmEnrollmentFormSubmissionId) {
        this.iccmEnrollmentFormSubmissionId = iccmEnrollmentFormSubmissionId;
    }
}
