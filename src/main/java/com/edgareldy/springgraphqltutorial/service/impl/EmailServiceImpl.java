package com.edgareldy.springgraphqltutorial.service.impl;

import com.edgareldy.springgraphqltutorial.email.EmailTemplateName;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED;

/**
 * Default {@link EmailService} implementation using {@link JavaMailSender} and
 * {@link SpringTemplateEngine} to render HTML email bodies from Thymeleaf
 * templates located in {@code src/main/resources/templates/}. Template names
 * are resolved via {@link EmailTemplateName} to avoid hardcoded strings in the
 * service methods. Each method is annotated
 * {@code @Async("emailTaskExecutor")} so the SMTP call runs on the dedicated
 * pool, never on the calling thread. Email links target the frontend
 * ({@code app.frontend-url}) so the user lands on the correct UI page before
 * the token is submitted to the API.
 * <p>
 * Created by edgar.muhamyangabo on 11/13/26
 * Author : edgar.muhamyangabo
 * Date : 11/13/26
 * Project : spring-graphql-tutorial
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String frontendUrl;
    private final String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender,
                            SpringTemplateEngine templateEngine,
                            @Value("${app.frontend-url}") String frontendUrl,
                            @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.frontendUrl = frontendUrl;
        this.fromAddress = fromAddress;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendActivationEmail(User user, String token) {
        String link = frontendUrl + "/auth/activate-account?token=" + token;
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("username", user.getFirstName());
        context.setVariable("activationLink", link);
        String html = templateEngine.process(EmailTemplateName.ACTIVATE_ACCOUNT.getName(), context);
        sendEmail(user.getEmail(), "Activate your account", html);
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(User user, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("username", user.getFirstName());
        context.setVariable("resetLink", link);
        String html = templateEngine.process(EmailTemplateName.RESET_PASSWORD.getName(), context);
        sendEmail(user.getEmail(), "Reset your password", html);
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MULTIPART_MODE_MIXED,
                    UTF_8.name()
            );
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Sent email to {} with subject '{}'", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {} - {}", to, e.getMessage());
        }
    }
}
