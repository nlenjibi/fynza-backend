package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.config.TestSecurityConfig;
import com.aoms.aomsbackend.attendance.entity.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestSecurityConfig.class)
@ExtendWith(SpringExtension.class)
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void findLocationSubtreeEmployeeIds_returnsRootAndFiveDescendantsMax() {
        UUID buildingId = UUID.randomUUID();
        Employee root = saveEmployee(buildingId, null, "Root", "Manager", "engineering");
        Employee level1 = saveEmployee(buildingId, root.getId(), "Level", "One", "engineering");
        Employee level2 = saveEmployee(buildingId, level1.getId(), "Level", "Two", "engineering");
        Employee level3 = saveEmployee(buildingId, level2.getId(), "Level", "Three", "engineering");
        Employee level4 = saveEmployee(buildingId, level3.getId(), "Level", "Four", "engineering");
        Employee level5 = saveEmployee(buildingId, level4.getId(), "Level", "Five", "engineering");
        Employee level6 = saveEmployee(buildingId, level5.getId(), "Level", "Six", "engineering");

        List<String> ids = employeeRepository.findLocationSubtreeEmployeeIds(buildingId, null, root.getId());
        Set<UUID> idSet = ids.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toSet());

        assertThat(ids).hasSize(6);
        assertThat(idSet)
                .contains(root.getId(), level1.getId(), level2.getId(), level3.getId(), level4.getId(), level5.getId())
                .doesNotContain(level6.getId());
    }

    @Test
    void findLocationSubtreeEmployeeIds_handlesCycleWithoutInfiniteLoop() {
        UUID buildingId = UUID.randomUUID();
        Employee employeeA = saveEmployee(buildingId, null, "Cycle", "A", "ops");
        Employee employeeB = saveEmployee(buildingId, employeeA.getId(), "Cycle", "B", "ops");

        employeeA.setManagerId(employeeB.getId());
        employeeRepository.save(employeeA);

        List<String> ids = employeeRepository.findLocationSubtreeEmployeeIds(buildingId, null, employeeA.getId());

        assertThat(ids)
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo(6)
                .contains(employeeA.getId().toString(), employeeB.getId().toString());
    }

    @Test
    void findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment_filtersDepartmentInDatabase() {
        UUID buildingId = UUID.randomUUID();
        Employee engineering = saveEmployee(buildingId, null, "Eng", "One", "Engineering");
        Employee hr = saveEmployee(buildingId, null, "Hr", "Two", "HR");

        List<UUID> ids = employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(buildingId, "HR");

        assertThat(ids)
                .hasSizeLessThanOrEqualTo(1)
                .containsExactly(hr.getId())
                .doesNotContain(engineering.getId());
    }

    private Employee saveEmployee(UUID buildingId, UUID managerId, String firstName, String lastName, String department) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setEmployeeCode("EMP-" + employee.getId());
        employee.setSsoUserId("sso-" + employee.getId());
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(employee.getId() + "@example.com");
        employee.setPrimaryBuildingId(buildingId);
        employee.setManagerId(managerId);
        employee.setDepartment(department);
        employee.setEmploymentStartDate(LocalDate.of(2024, 1, 1));
        employee.setActive(true);
        employee.setCreatedAt(Instant.parse("2026-04-28T00:00:00Z"));
        employee.setUpdatedAt(Instant.parse("2026-04-28T00:00:00Z"));
        return employeeRepository.save(employee);
    }
}
