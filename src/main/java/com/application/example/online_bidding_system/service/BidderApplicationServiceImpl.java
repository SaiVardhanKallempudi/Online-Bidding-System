package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.request.BidderApplicationRequest;
import com.application.example.online_bidding_system.dto.response.BidderApplicationResponse;
import com.application.example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system.exception.BadRequestException;
import com.application.example.online_bidding_system.exception.ResourceNotFoundException;
import com.application.example.online_bidding_system.repository.BidderApplicationRepository;
import com.application.example.online_bidding_system.repository.EmailOtpRepository;
import com.application.example.online_bidding_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BidderApplicationServiceImpl implements BidderApplicationService {

    @Autowired
    private BidderApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private Emailservice emailService;

    @Autowired
    private NotificationTriggerService notificationTrigger; // ✅ already wired — now used

    @Override
    public ResponseEntity<BidderApplicationResponse> applyAsBidder(BidderApplicationRequest request) {
        User user = userRepository.findByCollageId(request.getCollageId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "collegeId", request.getCollageId()));

        if (request.getStudentName() == null || request.getStudentName().trim().isEmpty())
            throw new BadRequestException("Student name is required");

        if (request.getStudentEmail() == null ||
                !request.getStudentEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            throw new BadRequestException("Invalid email format");

        String phone = String.valueOf(request.getPhoneNumber());
        if (!phone.matches("^[6-9]\\d{9}$"))
            throw new BadRequestException("Invalid phone number. Must be 10 digits starting with 6-9");

        if (!request.isTermsAccepted())
            throw new BadRequestException("You must accept the terms and conditions");

        if (user.isEmailVerified()) {
            System.out.println("✅ Email already verified — skipping OTP check");
        } else {
            if (request.getOtp() == null || request.getOtp().trim().isEmpty())
                throw new BadRequestException("OTP is required for unverified emails");

            Optional<EmailOtp> otpOptional = emailOtpRepository.findTopByEmailOrderByCreatedAtDesc(request.getStudentEmail());
            if (otpOptional.isEmpty())
                throw new BadRequestException("No OTP found for this email. Please request OTP first.");

            EmailOtp savedOtp = otpOptional.get();
            if (!savedOtp.isVerified())
                throw new BadRequestException("Email not verified. Please verify OTP first.");

            long diff = System.currentTimeMillis() - savedOtp.getCreatedAt().getTime();
            if (diff > 5 * 60 * 1000)
                throw new BadRequestException("OTP expired. Please request a new OTP.");

            if (!savedOtp.getOtp().equals(request.getOtp()))
                throw new BadRequestException("Invalid OTP.");

            user.setEmailVerified(true);
            userRepository.save(user);
        }

        if (applicationRepository.findByUser(user).isPresent())
            throw new BadRequestException("You have already applied. Check your application status.");

        BidderApplication application = new BidderApplication();
        application.setUser(user);
        application.setPhoneNumber(request.getPhoneNumber());
        application.setStatus(Status.PENDING);
        application.setReason(request.getReason());
        application.setPreferredStallCategory(request.getPreferredStallCategory());
        application.setAppliedAt(new Timestamp(System.currentTimeMillis()));
        applicationRepository.save(application);

        // ✅ Confirm application received
        notificationTrigger.notifyApplicationReceived(user.getStudentId());

        System.out.println("✅ Bidder application submitted for: " + user.getStudentEmail());

        BidderApplicationResponse response = convertToResponse(application);
        response.setStudentEmail(request.getStudentEmail());
        response.setStudentName(request.getStudentName());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<String> approveApplication(Long applicationId) {
        BidderApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (application.getStatus() != Status.PENDING)
            throw new BadRequestException("Application already processed. Status: " + application.getStatus());

        application.setStatus(Status.APPROVED);
        application.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        applicationRepository.save(application);

        User user = application.getUser();
        user.setRole(Role.BIDDER);
        userRepository.save(user);

        // ✅ In-app notification
        notificationTrigger.notifyApplicationApproved(user.getStudentId());

        // Email
        try {
            emailService.sendApplicationApprovedEmail(user.getStudentEmail(), user.getStudentName());
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e.getMessage());
        }

        System.out.println("✅ Application approved for: " + user.getStudentEmail());
        return ResponseEntity.ok("Application approved successfully. User is now a BIDDER.");
    }

    @Override
    public ResponseEntity<String> rejectApplication(Long applicationId) {
        return rejectApplicationWithReason(applicationId, "No specific reason provided");
    }

    public ResponseEntity<String> rejectApplicationWithReason(Long applicationId, String reason) {
        BidderApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (application.getStatus() != Status.PENDING)
            throw new BadRequestException("Application already processed. Status: " + application.getStatus());

        application.setStatus(Status.REJECTED);
        application.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        application.setRejectionReason(reason);
        applicationRepository.save(application);

        User user = application.getUser();

        // ✅ In-app notification
        notificationTrigger.notifyApplicationRejected(user.getStudentId(), reason);

        // Email
        try {
            emailService.sendApplicationRejectedEmail(user.getStudentEmail(), user.getStudentName(), reason);
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }

        System.out.println("❌ Application rejected for: " + user.getStudentEmail());
        return ResponseEntity.ok("Application rejected.");
    }

    @Override
    public ResponseEntity<String> updateApplicationStatus(Long applicationId, Status newStatus) {
        BidderApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        Status oldStatus = application.getStatus();
        application.setStatus(newStatus);
        application.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        applicationRepository.save(application);

        User user = application.getUser();

        if (newStatus == Status.APPROVED) {
            user.setRole(Role.BIDDER);
            userRepository.save(user);
            // ✅ Notify approval via status update path too
            notificationTrigger.notifyApplicationApproved(user.getStudentId());
            try {
                emailService.sendApplicationApprovedEmail(user.getStudentEmail(), user.getStudentName());
            } catch (Exception e) {
                System.err.println("Failed to send email: " + e.getMessage());
            }
        } else if (newStatus == Status.REJECTED) {
            // ✅ Notify rejection via status update path too
            notificationTrigger.notifyApplicationRejected(user.getStudentId(), application.getRejectionReason());
        }

        return ResponseEntity.ok("Status updated from " + oldStatus + " to " + newStatus);
    }

    @Override
    public String getApplicationStatusByEmail(String email) {
        return applicationRepository.findByUser_StudentEmail(email)
                .map(app -> app.getStatus().name())
                .orElse("NOT_APPLIED");
    }

    @Override
    public List<BidderApplicationResponse> getApplicationsByStatus(Status status) {
        return applicationRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Status hasUserAlreadyApplied(String studentEmail) {
        if (studentEmail == null || studentEmail.isEmpty()) return Status.NOT_APPLIED;
        Optional<User> userOpt = userRepository.findByStudentEmail(studentEmail);
        if (userOpt.isEmpty()) return Status.NOT_APPLIED;
        return applicationRepository.findByUser(userOpt.get())
                .map(BidderApplication::getStatus)
                .orElse(Status.NOT_APPLIED);
    }

    @Override
    public List<BidderApplicationResponse> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BidderApplicationResponse> getAllUsers() {
        return applicationRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private BidderApplicationResponse convertToResponse(BidderApplication app) {
        BidderApplicationResponse response = new BidderApplicationResponse();
        response.setApplicationId(app.getApplicationId());
        User user = app.getUser();
        if (user != null) {
            response.setUserId(user.getStudentId());
            response.setStudentName(user.getStudentName());
            response.setStudentEmail(user.getStudentEmail());
            response.setCollageId(user.getCollageId());
            response.setDepartment(user.getDepartment());
            response.setYear(user.getYear());
            response.setGender(user.getGender());
            response.setPhone(user.getPhone());
            response.setProfilePicture(user.getProfilePicture());
        }
        response.setPhoneNumber(app.getPhoneNumber());
        response.setStatus(app.getStatus());
        response.setReason(app.getReason());
        response.setPreferredStallCategory(app.getPreferredStallCategory());
        response.setAppliedAt(app.getAppliedAt());
        response.setReviewedAt(app.getReviewedAt());
        response.setRejectionReason(app.getRejectionReason());
        return response;
    }
}