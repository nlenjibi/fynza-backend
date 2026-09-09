package ecommerce.graphql.resolver.customer;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.customer.dto.request.CustomerSearchRequest;
import ecommerce.modules.customer.dto.response.*;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.service.CustomerAddressService;
import ecommerce.modules.customer.service.CustomerPreferenceService;
import ecommerce.modules.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CustomerGraphqlController {

    private final CustomerService           customerService;
    private final CustomerAddressService    addressService;
    private final CustomerPreferenceService preferenceService;

    // ── Self-service reads ────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAuthority('customer.read.own')")
    public CustomerDetailResponse meCustomer(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL meCustomer userId={}", principal.getId());
        return customerService.getMyCustomer(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('address.read.own')")
    public List<CustomerAddressResponse> myCustomerAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myCustomerAddresses userId={}", principal.getId());
        return addressService.getMyAddresses(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('customer.read.own')")
    public CustomerPreferenceResponse myCustomerPreferences(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myCustomerPreferences userId={}", principal.getId());
        return preferenceService.getMyPreferences(principal.getId());
    }

    // ── Admin reads ───────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAuthority('customer.read')")
    public CustomerDetailResponse customer(@Argument String id) {
        log.debug("GQL customer id={}", id);
        return customerService.getCustomerByPublicId(UUID.fromString(id));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('customer.read')")
    public Page<CustomerSummaryResponse> customers(
            @Argument Map<String, Object> pagination,
            @Argument Map<String, Object> filter) {

        log.debug("GQL customers filter={}", filter);
        CustomerSearchRequest params = new CustomerSearchRequest();
        if (filter != null) {
            if (filter.get("query")          != null) params.setQuery((String) filter.get("query"));
            if (filter.get("customerNumber") != null) params.setCustomerNumber((String) filter.get("customerNumber"));
            if (filter.get("status")         != null) params.setStatus(CustomerStatus.valueOf((String) filter.get("status")));
        }

        int page = pagination != null && pagination.get("page") != null ? (int) pagination.get("page") : 0;
        int size = pagination != null && pagination.get("size") != null ? (int) pagination.get("size") : 20;

        return customerService.searchCustomers(params, PageRequest.of(page, size));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('customer.read')")
    public List<CustomerStatusHistoryResponse> customerStatusHistory(@Argument String id) {
        log.debug("GQL customerStatusHistory id={}", id);
        return customerService.getStatusHistory(UUID.fromString(id));
    }
}
