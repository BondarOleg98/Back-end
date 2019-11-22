package com.netcraker.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class MailSender {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String username;

    public void send(String email, String subject, String messageCascade, String... params) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        String message = createMessage(messageCascade, params);

        mailMessage.setFrom(username);

        mailMessage.setTo(email);
        mailMessage.setText(message);
        mailMessage.setSubject(subject);

        mailSender.send(mailMessage);
    }

    @SuppressWarnings({"all"})
    private String createMessage(String messageCascade, String... params) {
        return String.format(messageCascade, params);

    }
}
