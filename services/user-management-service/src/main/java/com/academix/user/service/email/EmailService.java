package com.academix.user.service.email;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
