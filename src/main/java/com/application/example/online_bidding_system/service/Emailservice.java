package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.email.EmailDetails;
import com.application.example.online_bidding_system.entity.EmailOtp;
import com.application.example.online_bidding_system.repository.EmailOtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class Emailservice {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /* ----------------------------------------------------
       COMMON EMAIL SENDER (BREVO API)
    ---------------------------------------------------- */
    private void sendHtmlEmail(String to, String subject, String html) {

        String url = "https://api.brevo.com/v3/smtp/email";

        String payload = """
        {
          "sender": {
            "name": "%s",
            "email": "%s"
          },
          "to": [{
            "email": "%s"
          }],
          "subject": "%s",
          "htmlContent": %s
        }
        """.formatted(fromName, fromEmail, to, subject, toJsonString(html));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Email sent to: " + to);
            } else {
                System.err.println("❌ Brevo error: " + response.getBody());
            }

        } catch (Exception e) {
            System.err.println("❌ Email failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ----------------------------------------------------
       OTP EMAIL
    ---------------------------------------------------- */
    public void sendOtpEmail(String toEmail, String otp) {

        String html = """
        <html>
        <body style="font-family:Arial;background:#f4f4f4;padding:20px;">
          <div style="max-width:600px;margin:auto;background:#fff;
                      border-radius:10px;padding:30px;text-align:center">
            <h2>🔐 Email Verification</h2>
            <p>Your OTP:</p>
            <h1 style="letter-spacing:8px">%s</h1>
            <p>Valid for 5 minutes</p>
          </div>
        </body>
        </html>
        """.formatted(otp);

        sendHtmlEmail(toEmail, "🔐 OTP Verification", html);
    }

    public void sendBiddingStartedEmail(String toEmail, String studentName, String stallName) {
        String html = """
        <html><body>
        <h2>Hello %s 👋</h2>
        <p>Bidding has started for:</p>
        <h2>%s</h2>
        </body></html>
        """.formatted(studentName, stallName);

        sendHtmlEmail(toEmail, "🔔 Bidding Started - " + stallName, html);
    }

    public void sendOutbidEmail(String toEmail, String studentName, String stallName, String highestBid) {
        String html = """
        <html><body>
        <h2>⚠️ You've Been Outbid</h2>
        <p>Hello %s</p>
        <p>Stall: <strong>%s</strong></p>
        <p>New Highest Bid: ₹%s</p>
        </body></html>
        """.formatted(studentName, stallName, highestBid);

        sendHtmlEmail(toEmail, "⚠️ Outbid Alert - " + stallName, html);
    }

    public void sendSimpleMail(EmailDetails details) {
        sendHtmlEmail(details.getTo(), details.getSubject(),
                "<pre>" + details.getBody() + "</pre>");
    }

    public void sendApplicationApprovedEmail(String toEmail, String studentName) {
        String html = """
        <html><body>
        <h2>🎉 Application Approved!</h2>
        <p>Hello %s</p>
        <p>You are now approved to start bidding.</p>
        </body></html>
        """.formatted(studentName);

        sendHtmlEmail(toEmail, "🎉 Application Approved", html);
    }

    public void sendApplicationRejectedEmail(String toEmail, String studentName, String reason) {
        String html = """
        <html><body>
        <h2>❌ Application Update</h2>
        <p>Hello %s</p>
        <p>Reason: %s</p>
        </body></html>
        """.formatted(studentName, reason);

        sendHtmlEmail(toEmail,
                "Application Status Update – College Bidding System", html);
    }

    public void sendWinnerEmail(String toEmail, String studentName, String stallName, String winningAmount) {
        String html = """
        <html><body style="text-align:center">
        <h1>🏆 Congratulations %s!</h1>
        <p>Stall: %s</p>
        <h2>₹%s</h2>
        </body></html>
        """.formatted(studentName, stallName, winningAmount);

        sendHtmlEmail(toEmail, "🏆 You Won the Bid!", html);
    }

    public Optional<EmailOtp> getLatestVerifiedOtp(String email) {
        return emailOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .filter(EmailOtp::isVerified);
    }

    /* ----------------------------------------------------
       JSON SAFE ENCODER
    ---------------------------------------------------- */
    private static String toJsonString(String text) {
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "")
                .replace("\r", "") + "\"";
    }
}
