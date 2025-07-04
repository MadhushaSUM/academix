package com.academix.user.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * A placeholder EmailService that prints email content to the console.
 * Suitable for development environments.
 * Use `@Profile("dev")` or similar to activate only in dev.
 */
@Service
@Profile("dev") // Activate this service only when 'dev' profile is active
@Slf4j
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("--- SIMULATING EMAIL SEND ---");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body:\n{}", body);
        log.info("--- END EMAIL SIMULATION ---");
    }
}