package com.edgareldy.springgraphqltutorial.service;

import com.edgareldy.springgraphqltutorial.entity.User;

/**
 * Contract for sending transactional emails (account activation, password
 * reset). Uses Thymeleaf HTML templates and JavaMailSender for rich HTML
 * emails. Implementations are expected to be asynchronous so callers are
 * not blocked on SMTP delivery.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
public interface EmailService {

    /**
     * Sends an HTML account activation email to the given user.
     */
    void sendActivationEmail(User user, String activationToken);

    /**
     * Sends an HTML password reset email to the given user.
     */
    void sendPasswordResetEmail(User user, String resetToken);

}
