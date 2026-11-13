package com.edgareldy.springgraphqltutorial.service.impl;

import com.edgareldy.springgraphqltutorial.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails via JavaMailSender. Both methods are
 * @Async("emailTaskExecutor") so delivery never blocks the calling thread.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendActivationEmail(String to, String activationToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Activate your account");
        message.setText("Use the following token to activate your account: " + activationToken);
        mailSender.send(message);
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String to, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset your password");
        message.setText("Use the following token to reset your password: " + resetToken);
        mailSender.send(message);
    }

}
