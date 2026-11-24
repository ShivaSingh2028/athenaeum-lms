package com.college.library.communication;

public interface EmailSender {
    void send(String recipient, String subject, String bodyHtml) throws Exception;
}
