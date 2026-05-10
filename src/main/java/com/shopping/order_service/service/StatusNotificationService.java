package com.shopping.order_service.service;

import com.shopping.order_service.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusNotificationService {

    private final RestTemplate restTemplate;

    @Value("${services.email-service}")
    private String emailServiceUrl;

    @Value("${services.user-service}")
    private String userServiceUrl;

    /**
     * Sends a STATUS_UPDATE email to the customer after an admin changes order status.
     * Runs asynchronously — never blocks the admin response.
     */
    @Async
    public void sendStatusUpdateEmail(OrderResponse order) {
        try {
            // Fetch customer email from user-service
            String customerEmail = getCustomerEmail(order.getCustomerId());
            if (customerEmail == null) {
                log.warn("Could not fetch email for customer {} — skipping status notification",
                        order.getCustomerId());
                return;
            }

            Map<String, Object> emailRequest = new HashMap<>();
            emailRequest.put("to",          customerEmail);
            emailRequest.put("subject",     "Order Update — Your order is now " + order.getStatus());
            emailRequest.put("type",        "STATUS_UPDATE");
            emailRequest.put("orderId",     String.valueOf(order.getOrderId()));
            emailRequest.put("orderStatus", order.getStatus().name());
            emailRequest.put("body",        buildBody(order));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForEntity(
                    emailServiceUrl + "/api/email/send",
                    new HttpEntity<>(emailRequest, headers),
                    String.class
            );

            log.info("Status update email sent for order {} → {}",
                    order.getOrderId(), order.getStatus());

        } catch (Exception e) {
            log.warn("Failed to send status update email for order {}: {}",
                    order.getOrderId(), e.getMessage());
        }
    }

    private String getCustomerEmail(Long customerId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    userServiceUrl + "/api/users/" + customerId, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("email");
            }
        } catch (Exception e) {
            log.warn("Could not fetch customer {}: {}", customerId, e.getMessage());
        }
        return null;
    }

    private String buildBody(OrderResponse order) {
        return "Your order #" + order.getOrderId()
                + " status has been updated to: " + order.getStatus().name()
                + ".\n\nThank you for shopping with us!";
    }
}
