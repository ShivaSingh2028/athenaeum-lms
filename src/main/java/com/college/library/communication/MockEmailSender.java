package com.college.library.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnMissingBean(type = "org.springframework.mail.javamail.JavaMailSender")
public class MockEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);

    public record SentMessage(String recipient, String subject, String bodyHtml) {}

    private final List<SentMessage> sentMessages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void send(String recipient, String subject, String bodyHtml) throws Exception {
        log.info("[EMAIL DISPATCH] To: {} | Subject: {}", recipient, subject);
        sentMessages.add(new SentMessage(recipient, subject, bodyHtml));
    }

    public List<SentMessage> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public void clear() {
        sentMessages.clear();
    }
}
