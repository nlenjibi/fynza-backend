package com.aoms.aomsbackend.attendance.client;

import com.aoms.aomsbackend.attendance.dto.*;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.config.ArmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmsGraphqlClient {

    private final ArmProperties armProperties;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    private static final String QUERY = "query";
    private static final String VARIABLES = "variables";

    // Queries for attendance data
    private static final String GET_APPROVED_OOO_REQUESTS = """
            query GetApprovedOooRequests($locationId: ID!, $date: Date!) {
              approvedOooRequests(locationId: $locationId, date: $date) {
                requestId
                userId
                startDate
                endDate
                locationId
                status
              }
            }
            """;

    private static final String GET_APPROVED_REMOTE_REQUESTS = """
            query GetApprovedRemoteRequests($locationId: ID!, $date: Date!) {
              approvedRemoteRequests(locationId: $locationId, date: $date) {
                requestId
                userId
                dates
                locationId
                status
              }
            }
            """;

    private static final String LIST_HOLIDAYS_BY_COUNTRY = """
            query ListHolidaysByCountry($country: String!) {
              listHolidaysByCountry(country: $country) {
                id
                title
                description
                start_day
                end_day
                country
                is_archived
              }
            }
            """;

    public List<ArmsOooRequest> getApprovedOooRequests(String locationId, LocalDate date) {
        try {
            Map<String, Object> body = Map.of(
                    QUERY, GET_APPROVED_OOO_REQUESTS,
                VARIABLES, Map.of("locationId", locationId, "date", date.toString())
            );
            return parseOooRequests(callGraphQl(body));
        } catch (ResourceAccessException ex) {
            log.warn("ARMS timeout fetching OOO requests for location={}, applying fallback", locationId);
            return List.of();
        } catch (Exception ex) {
            log.warn("ARMS unavailable fetching OOO requests for location={}: {}", locationId, ex.getMessage());
            return List.of();
        }
    }

    public List<ArmsRemoteRequest> getApprovedRemoteRequests(String locationId, LocalDate date) {
        try {
            Map<String, Object> body = Map.of(
                    QUERY, GET_APPROVED_REMOTE_REQUESTS,
                VARIABLES, Map.of("locationId", locationId, "date", date.toString())
            );
            return parseRemoteRequests(callGraphQl(body));
        } catch (ResourceAccessException ex) {
            log.warn("ARMS timeout fetching remote requests for location={}, applying fallback", locationId);
            return List.of();
        } catch (Exception ex) {
            log.warn("ARMS unavailable fetching remote requests for location={}: {}", locationId, ex.getMessage());
            return List.of();
        }
    }

    public List<ArmsPublicHolidayResponse> getPublicHolidays(String country) {
        try {
            Map<String, Object> body = Map.of(
                    QUERY, LIST_HOLIDAYS_BY_COUNTRY,
                VARIABLES, Map.of("country", country)
            );
            return parsePublicHolidays(callGraphQl(body));
        } catch (ResourceAccessException ex) {
            log.warn("ARMS timeout fetching public holidays for country={}, applying fallback", country);
            return List.of();
        } catch (Exception ex) {
            log.warn("ARMS unavailable fetching public holidays for country={}: {}", country, ex.getMessage());
            return List.of();
        }
    }

    private String callGraphQl(Map<String, Object> body) {
        RestClient restClient = buildRestClient();
        return restClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String.class);
    }

    private List<ArmsOooRequest> parseOooRequests(String responseJson) {
        try {
            JsonNode data = objectMapper.readTree(responseJson)
                .path("data").path("approvedOooRequests");

            if (data.isMissingNode() || data.isNull()) {
                log.warn("ARMS returned empty approvedOooRequests");
                return List.of();
            }

            return objectMapper.convertValue(data, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ArmsOooRequest.class));
        } catch (Exception ex) {
            log.warn("Failed to parse ARMS OOO requests response: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<ArmsRemoteRequest> parseRemoteRequests(String responseJson) {
        try {
            JsonNode data = objectMapper.readTree(responseJson)
                .path("data").path("approvedRemoteRequests");

            if (data.isMissingNode() || data.isNull()) {
                log.warn("ARMS returned empty approvedRemoteRequests");
                return List.of();
            }

            return objectMapper.convertValue(data, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ArmsRemoteRequest.class));
        } catch (Exception ex) {
            log.warn("Failed to parse ARMS remote requests response: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<ArmsPublicHolidayResponse> parsePublicHolidays(String responseJson) {
        try {
            JsonNode data = objectMapper.readTree(responseJson)
                .path("data").path("listHolidaysByCountry");

            if (data.isMissingNode() || data.isNull()) {
                log.warn("ARMS returned empty publicHolidays");
                return List.of();
            }

            return objectMapper.convertValue(data, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ArmsPublicHolidayResponse.class));
        } catch (Exception ex) {
            log.warn("Failed to parse ARMS public holidays response: {}", ex.getMessage());
            return List.of();
        }
    }

    private RestClient buildRestClient() {
        String token = getTokenFromSession();
        
        return RestClient.builder()
            .baseUrl(armProperties.getGraphqlUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
    }

    private String getTokenFromSession() {

        String token = (String) request.getSession(false).getAttribute(SessionAttribute.ACCESS_TOKEN.getKey());
        if (token == null || token.isBlank()) {
            String serviceToken = armProperties.getServiceToken();
            if (serviceToken != null && !serviceToken.isBlank()) {
                log.info("Session token not found, using service token for ARMS GraphQL calls");
                return serviceToken;
            }
            log.warn("No access token available for ARMS GraphQL calls");
            throw new IllegalStateException("Access token not available. Please login first.");
        }

        return token;
    }
}