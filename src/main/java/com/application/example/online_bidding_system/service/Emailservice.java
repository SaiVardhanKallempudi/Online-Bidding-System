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
       Modern, Branded Email Templates for All Senders
    ---------------------------------------------------- */

    public void sendOtpEmail(String toEmail, String otp) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#f6f9fc;padding:32px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 2px 16px #3b82f65b;padding:40px;">
            <img src="https://cdn.jsdelivr.net/gh/saivardhan190/bidmart-brand@main/bidmart-logo.png" alt="BidMart" height="40" style="margin-bottom:24px;margin-left:-8px;">
            <h2 style="color:#2737e6;margin-bottom:8px;">Verify Your Email Address</h2>
            <p style="color:#333;font-size:16px;">Hello,</p>
            <p style="color:#555;font-size:14px;margin-bottom:24px;">
              Please use the code below to verify your email and activate your account.<br /><br />
              <span style="display:inline-block;background:#eaedfa;color:#2737e6;border-radius:8px;font-size:22px;font-weight:600;padding:18px 24px;letter-spacing:8px;">
                %s
              </span>
            </p>
            <p style="font-size:13px;color:#aaa;">This code is valid for five minutes.<br>
             If you did not request this, please ignore this email.</p>
            <hr style="margin:32px 0;border-top:1px solid #eee;">
            <p style="color:#888;font-size:13px;">
             For queries or support, please reply to <strong>support.bidmart@bidmart.me</strong>.
            </p>
          </div>
        </body>
        </html>
        """.formatted(otp);
        sendHtmlEmail(toEmail, "BidMart | Email Verification Code", html);
    }

    public void sendBiddingStartedEmail(String toEmail, String studentName, String stallName) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#f6f9fc;padding:32px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 2px 16px #3b82f65b;padding:40px;">
            <img src="https://cdn.jsdelivr.net/gh/saivardhan190/bidmart-brand@main/bidmart-logo.png" alt="BidMart" height="40" style="margin-bottom:18px;">
            <h2 style="color:#2737e6;margin-bottom:8px;">Bidding Started</h2>
            <p style="color:#222;font-size:16px;">Hi %s,</p>
            <p style="color:#555;font-size:14px;margin-bottom:24px;">Bidding has started for <span style="font-weight:600;">%s</span>.</p>
            <img src="https://cdn-icons-png.flaticon.com/512/2170/2170147.png" width="80" alt="Bidding Icon" style="margin-bottom:28px;">
            <hr style="margin:32px 0;border-top:1px solid #eee;">
            <p style="color:#aaa;font-size:13px;">Log in to place your bid.<br>For help: support.bidmart@bidmart.me</p>
          </div>
        </body>
        </html>
        """.formatted(studentName, stallName);
        sendHtmlEmail(toEmail, "BidMart | Bidding Started: " + stallName, html);
    }

    public void sendOutbidEmail(String toEmail, String studentName, String stallName, String highestBid) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#f6f9fc;padding:32px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 4px 16px #ffb70335;padding:40px;">
            <img src="https://cdn.jsdelivr.net/gh/saivardhan190/bidmart-brand@main/bidmart-logo.png" alt="BidMart" height="40" style="margin-bottom:22px;">
            <h2 style="color:#fb8500;margin-bottom:8px;">Outbid Alert!</h2>
            <p style="color:#222;font-size:15px;">Hi %s,</p>
            <p style="color:#555;font-size:14px;margin-bottom:24px;">You have been outbid on <span style="font-weight:600;">%s</span>.</p>
            <div style="background:#faedcd;border-radius:8px;padding:16px;margin-bottom:22px;">
              <span style="font-size:18px;color:#fb8500;font-weight:600;">New Highest Bid: ₹%s</span>
            </div>
            <hr style="margin:32px 0;border-top:1px solid #eee;">
            <p style="color:#aaa;font-size:13px;">Log in to place a higher bid.<br>For help: support.bidmart@bidmart.me</p>
          </div>
        </body>
        </html>
        """.formatted(studentName, stallName, highestBid);
        sendHtmlEmail(toEmail, "BidMart | Outbid Alert: " + stallName, html);
    }

    public void sendApplicationApprovedEmail(String toEmail, String studentName) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#f6f9fc;padding:32px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 2px 16px #10b98135;padding:40px;">
            <img src="https://cdn.jsdelivr.net/gh/saivardhan190/bidmart-brand@main/bidmart-logo.png" alt="BidMart" height="40" style="margin-bottom:22px;">
            <h2 style="color:#10b981;">Application Approved!</h2>
            <p style="color:#222;font-size:16px;">Hi %s,</p>
            <p style="color:#555;font-size:14px;margin-bottom:24px;">Your bidder application has been approved. Welcome to BidMart!</p>
            <a href="http://localhost:4200/stalls" style="display:inline-block;background:#10b981;color:#fff;padding:12px 32px;border-radius:8px;font-weight:600;text-decoration:none;margin-top:18px;">Start Bidding</a>
            <hr style="margin:32px 0;border-top:1px solid #eee;">
            <p style="color:#aaa;font-size:13px;">For help: support.bidmart@bidmart.me</p>
          </div>
        </body>
        </html>
        """.formatted(studentName);
        sendHtmlEmail(toEmail, "BidMart | Application Approved", html);
    }

    public void sendApplicationRejectedEmail(String toEmail, String studentName, String reason) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#f6f9fc;padding:32px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 2px 16px #ef444417;padding:40px;">
            <img src="https://cdn.jsdelivr.net/gh/saivardhan190/bidmart-brand@main/bidmart-logo.png" alt="BidMart" height="40" style="margin-bottom:22px;">
            <h2 style="color:#ef4444;">Application Rejected</h2>
            <p style="color:#222;font-size:16px;">Hi %s,</p>
            <div style="background:#fee2e2;border-radius:8px;padding:12px;margin:18px 0;color:#ff5e5e;font-weight:500;">
              Reason: %s
            </div>
            <p style="color:#888;font-size:13px;">If you believe this was a mistake, reply to support@bidmart.me.</p>
          </div>
        </body>
        </html>
        """.formatted(studentName, reason);
        sendHtmlEmail(toEmail, "BidMart | Application Rejected", html);
    }

    public void sendWinnerEmail(String toEmail, String studentName, String stallName, String winningAmount) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#f6f9fc;padding:32px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 2px 16px #38bdf831;padding:40px;">
            <img src="https://cdn.jsdelivr.net/gh/saivardhan190/bidmart-brand@main/bidmart-logo.png" alt="BidMart" height="40" style="margin-bottom:22px;">
            <h2 style="color:#38bdf8;">🏆 Congratulations!</h2>
            <p style="color:#222;font-size:16px;">Dear %s,</p>
            <p style="color:#555;font-size:14px;">You have won the bid for <b>%s</b>.</p>
            <div style="background:#f0f9ff;border-radius:8px;padding:16px;margin:18px 0;">
              <span style="font-size:20px;color:#2b93ed;font-weight:600;">Winning Bid: %s</span>
            </div>
            <hr style="margin:32px 0;border-top:1px solid #eee;">
            <p style="color:#aaa;font-size:13px;">Check your dashboard for details. Any queries? support@bidmart.me</p>
          </div>
        </body>
        </html>
        """.formatted(studentName, stallName, winningAmount);
        sendHtmlEmail(toEmail, "BidMart | Winner Announcement", html);
    }

    public void sendSimpleMail(EmailDetails details) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#fff;padding:32px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 2px 16px #4251e627;padding:40px;">
            <h2 style="color:#2737e6;">BidMart Notification</h2>
            <pre style="background:#f6f8fa;border-radius:8px;padding:16px;font-weight:500;color:#222">%s</pre>
            <hr style="margin:18px 0 0 0;border-top:1px solid #eee;">
            <p style="color:#aaa;font-size:13px;">BidMart Team</p>
          </div>
        </body>
        </html>
        """.formatted(details.getBody());
        sendHtmlEmail(details.getTo(), details.getSubject(), html);
    }

    public void sendPasswordResetOtpEmail(String toEmail, String studentName, String otp) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#e2eafc;padding:24px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 4px 18px #3b82f615;padding:40px;">
            <h2 style="color:#1e3a8a;">Password Reset Request</h2>
            <p style="color:#222;font-size:16px;">Hi %s,</p>
            <p style="color:#555;font-size:14px;">Your password reset code is:</p>
            <div style="background:#e0e7ff;border-radius:8px;color:#1e3a8a;font-size:24px;font-weight:600;padding:16px 28px;letter-spacing:7px;display:inline-block;margin:24px 0;">
              %s
            </div>
            <p style="color:#999;font-size:13px;">Code valid for 5 minutes.<br />If you didn't request this, ignore this email.</p>
            <hr style="margin:32px 0;border-top:1px solid #eee;">
            <p style="color:#aaa;font-size:13px;">BidMart password support: support@bidmart.me</p>
          </div>
        </body>
        </html>
        """.formatted(studentName, otp);
        sendHtmlEmail(toEmail, "BidMart | Password Reset Code", html);
    }

    public void sendPasswordResetSuccessEmail(String toEmail, String studentName) {
        String html = """
        <html>
        <body style="font-family:Segoe UI,Arial;background:#f1f8f5;padding:24px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 4px 14px #10b98122;padding:40px;">
            <h2 style="color:#10b981;">Password Reset Successful</h2>
            <p style="color:#222;font-size:16px;">Hi %s,</p>
            <p style="color:#555;font-size:14px;">Your password was reset. You can now log in with your new password:</p>
            <a href="http://localhost:4200/login" style="display:inline-block;margin-top:18px;background:#2737e6;color:#fff;padding:12px 32px;border-radius:8px;font-weight:600;text-decoration:none;">
                Login Now
            </a>
            <hr style="margin:32px 0;border-top:1px solid #eee;">
            <p style="color:#aaa;font-size:13px;">BidMart Team</p>
          </div>
        </body>
        </html>
        """.formatted(studentName);
        sendHtmlEmail(toEmail, "BidMart | Password Reset Successful", html);
    }

    // Support for OTP retrieval
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