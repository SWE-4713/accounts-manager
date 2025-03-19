package com.example.FinanceProject.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Send simple text email
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("atlasfinances.notifs@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
        System.out.println("Email sent successfully to: " + to);
    }
    
    // Send HTML email
    public void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("atlasfinances.notifs@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true indicates HTML
        
        mailSender.send(message);
        System.out.println("HTML email sent successfully to: " + to);
    }
    
    // Send email with attachment
    public void sendEmailWithAttachment(String to, String subject, String body, 
                                       String attachmentPath, String attachmentName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true); // true indicates multipart message
        
        helper.setFrom("atlasfinances.notifs@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);
        
        FileSystemResource file = new FileSystemResource(new File(attachmentPath));
        helper.addAttachment(attachmentName, file);
        
        mailSender.send(message);
        System.out.println("Email with attachment sent successfully to: " + to);
    }
}

