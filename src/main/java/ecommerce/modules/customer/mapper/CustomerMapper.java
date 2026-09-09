package ecommerce.modules.customer.mapper;

import ecommerce.modules.customer.dto.response.*;
import ecommerce.modules.customer.entity.*;
import ecommerce.modules.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer, User user) {
        return CustomerResponse.builder()
                .id(customer.getPublicId())
                .customerNumber(customer.getCustomerNumber())
                .status(customer.getStatus())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public CustomerSummaryResponse toSummary(Customer customer, User user) {
        return CustomerSummaryResponse.builder()
                .id(customer.getPublicId())
                .customerNumber(customer.getCustomerNumber())
                .status(customer.getStatus())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    public CustomerDetailResponse toDetailResponse(Customer customer, User user,
                                                    CustomerPreference preference,
                                                    List<CustomerAddress> addresses) {
        return CustomerDetailResponse.builder()
                .id(customer.getPublicId())
                .customerNumber(customer.getCustomerNumber())
                .status(customer.getStatus())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .preferences(preference != null ? toPreferenceResponse(preference) : null)
                .addresses(addresses.stream().map(this::toAddressResponse).toList())
                .build();
    }

    public CustomerAddressResponse toAddressResponse(CustomerAddress address) {
        return CustomerAddressResponse.builder()
                .id(address.getPublicId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .region(address.getRegion())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .addressType(address.getAddressType())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public CustomerPreferenceResponse toPreferenceResponse(CustomerPreference pref) {
        return CustomerPreferenceResponse.builder()
                .id(pref.getPublicId())
                .language(pref.getLanguage())
                .currency(pref.getCurrency())
                .marketingOptIn(pref.getMarketingOptIn())
                .emailNotifications(pref.getEmailNotifications())
                .smsNotifications(pref.getSmsNotifications())
                .pushNotifications(pref.getPushNotifications())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }

    public CustomerSummaryResponse toSummary(CustomerSummaryView view) {
        return CustomerSummaryResponse.builder()
                .id(view.getPublicId())
                .customerNumber(view.getCustomerNumber())
                .status(view.getStatus())
                .firstName(view.getFirstName())
                .lastName(view.getLastName())
                .email(view.getEmail())
                .createdAt(view.getCreatedAt())
                .build();
    }

    public CustomerDetailResponse toDetailResponse(CustomerDetailView view, List<CustomerAddress> addresses) {
        CustomerPreferenceResponse pref = view.getPrefPublicId() != null
                ? CustomerPreferenceResponse.builder()
                        .id(view.getPrefPublicId())
                        .language(view.getLanguage())
                        .currency(view.getCurrency())
                        .marketingOptIn(view.getMarketingOptIn())
                        .emailNotifications(view.getEmailNotifications())
                        .smsNotifications(view.getSmsNotifications())
                        .pushNotifications(view.getPushNotifications())
                        .updatedAt(view.getPrefUpdatedAt())
                        .build()
                : null;
        return CustomerDetailResponse.builder()
                .id(view.getPublicId())
                .customerNumber(view.getCustomerNumber())
                .status(view.getStatus())
                .firstName(view.getFirstName())
                .lastName(view.getLastName())
                .email(view.getEmail())
                .phone(view.getPhone())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .preferences(pref)
                .addresses(addresses.stream().map(this::toAddressResponse).toList())
                .build();
    }

    public CustomerStatsResponse toStats(CustomerStatsView view) {
        return CustomerStatsResponse.builder()
                .totalCustomers(view.getTotalCustomers())
                .activeCustomers(view.getActiveCustomers())
                .newCustomersThisMonth(view.getNewCustomersThisMonth())
                .build();
    }

    public CustomerStatusHistoryResponse toStatusHistoryResponse(CustomerStatusHistory history) {
        return CustomerStatusHistoryResponse.builder()
                .id(history.getPublicId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .createdAt(history.getCreatedAt())
                .expiresAt(history.getExpiresAt())
                .build();
    }
}
