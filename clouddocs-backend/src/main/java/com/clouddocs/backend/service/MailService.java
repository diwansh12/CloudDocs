package com.clouddocs.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    // Placeholder: integrate with SMTP or provider (SendGrid, SES, etc.)
    public void send(String toEmail, String subject, String body) {
        // Implement real email sending; for now, just log
        logger.info("Email queued to {}: {}", toEmail, subject);
    }
}
