package com.academix.course.clients;

import com.academix.course.exception.ServiceCommunicationException;
import com.academix.common.models.dto.InternalUserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;

@Component
@Slf4j
public class UserManagementServiceClient {

    private final RestTemplate restTemplate;

    // These values will be injected from application.yml
    @Value("${lms.client.user-management-service.base-url}")
    private String userManagementServiceBaseUrl;

    @Value("${lms.client.user-management-service.api-key}")
    private String userManagementServiceApiKey;

    public UserManagementServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches user details from User Management Service by user ID.
     * @param userId The ID of the user to fetch.
     * @return An Optional containing InternalUserDto if found, or empty otherwise.
     */
    public Optional<InternalUserDto> getUserById(Long userId) {
        String url = userManagementServiceBaseUrl + "/api/internal/users/by-id/" + userId;
        return callUserManagementService(url, HttpMethod.GET, InternalUserDto.class);
    }

    /**
     * Fetches user details from User Management Service by username.
     * @param username The username of the user to fetch.
     * @return An Optional containing InternalUserDto if found, or empty otherwise.
     */
    public Optional<InternalUserDto> getUserByUsername(String username) {
        String url = userManagementServiceBaseUrl + "/api/internal/users/by-username?username=" + username;
        return callUserManagementService(url, HttpMethod.GET, InternalUserDto.class);
    }

    private <T> Optional<T> callUserManagementService(String url, HttpMethod method, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", userManagementServiceApiKey); // Add the internal API key for security
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers); // No request body needed for GET

        try {
            log.debug("Calling User Management Service: {} {}", method.name(), url);
            ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully received response from User Management Service.");
                return Optional.of(response.getBody());
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("User not found via User Management Service at {}", url);
                return Optional.empty();
            } else {
                log.error("Failed to get response from User Management Service. Status: {}", response.getStatusCode());
                throw new ServiceCommunicationException("Failed to get response from User Management Service. Status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Resource not found from User Management Service at {}: {}", url, e.getMessage());
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            log.error("Client error communicating with User Management Service at {}: Status: {}, Body: {}", url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceCommunicationException("Client error communicating with User Management Service: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error communicating with User Management Service at {}: {}", url, e.getMessage(), e);
            throw new ServiceCommunicationException("Error communicating with User Management Service: " + e.getMessage(), e);
        }
    }
}