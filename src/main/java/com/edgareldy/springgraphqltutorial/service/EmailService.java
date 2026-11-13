package com.edgareldy.springgraphqltutorial.service;

/**
 * Contract for sending transactional emails (account activation, password
 * reset). Implementations are expected to be asynchronous so callers are not
 * blocked on SMTP delivery.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
public interface EmailService {

    /**
     * Sends an account activation email containing the activation token.
     */
    void sendActivationEmail(String to, String activationToken);

    /**
     * Sends a password reset email containing the reset token.
     */
    void sendPasswordResetEmail(String to, String resetToken);

}
