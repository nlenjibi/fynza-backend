package com.aoms.aomsbackend.auth.dto;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ArmProfile.ArmProfileBuilder.class)
public class ArmProfile {

    // Bio fields (getEmployeeBioForExternalService — access-gated; null when not authorized)
    String fullName;         // full_name from bio
    String armEmail;         // email from bio (may differ from SSO email)

    // EmployeeInfo record identity
    String armsInfoId;       // getEmployeeActiveInfo.id
    String employeeId;       // employee_id  (e.g. NSP-OP-23-1285)
    String managerId;        // manager_id   (ARMS user_id of direct manager)
    String status;           // "active" | "inactive"  (derived from active boolean)
    String startingDate;     // starting_date (ISO-8601 date string)
    String endDate;          // end_date (nullable)

    // Role / classification
    String department;       // department.department_name
    String position;         // position.position_name
    String employeeType;     // employee_type.name

    // Location identifiers (stable IDs for AMS sync)
    String officeId;         // office_id
    String locationId;       // location_id
    String organizationId;   // organization_id

    // Resolved display names
    String office;           // office.name
    String location;         // location.country_name
    String town;             // location.town
    String branch;           // location.branch_name
    String organisation;     // organization.name

    // Manager (resolved from manager { id first_name last_name email })
    String managerName;      // manager.first_name + " " + manager.last_name
    String managerEmail;     // manager.email

    public static ArmProfile empty() {
        return ArmProfile.builder().build();
    }

    public boolean isEmpty() {
        return department == null && position == null && employeeType == null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ArmProfileBuilder {}
}
