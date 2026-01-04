package com.application.example.online_bidding_system. service;

import com.application.example. online_bidding_system.dto.request.BidderApplicationRequest;
import com.application.example. online_bidding_system.dto.response.BidderApplicationResponse;
import com.application.example. online_bidding_system.entity.*;
import com.application.example.online_bidding_system. exception.BadRequestException;
import com. application.example.online_bidding_system.exception.ResourceNotFoundException;
import com. application.example.online_bidding_system.repository. BidderApplicationRepository;
import com.application.example. online_bidding_system.repository.EmailOtpRepository;
import com.application.example. online_bidding_system.repository.UserRepository;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework. stereotype.Service;

import java.sql.Timestamp;
import java.util. List;
import java.util.Optional;
import java.util. stream.Collectors;

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

    @Override
    public ResponseEntity<BidderApplicationResponse> applyAsBidder(BidderApplicationRequest request) {
        // Validate user exists
        User user = userRepository. findByCollageId(request.getCollageId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "collegeId", request. getCollageId()));

        // Validate student name
        if (request.getStudentName() == null || request.getStudentName().trim().isEmpty()) {
            throw new BadRequestException("Student name is required");
        }

        // Validate email format
        if (request.getStudentEmail() == null ||
                !request.getStudentEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BadRequestException("Invalid email format");
        }

        // Validate phone number
        String phone = String.valueOf(request. getPhoneNumber());
        if (! phone.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Invalid phone number.  Must be 10 digits starting with 6-9");
        }

        // Validate OTP
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            throw new BadRequestException("OTP is required");
        }

        // Check OTP verification
        Optional<EmailOtp> otpOptional = emailOtpRepository. findTopByEmailOrderByCreatedAtDesc(request.getStudentEmail());
        if (otpOptional. isEmpty() || !otpOptional.get().isVerified()) {
            throw new BadRequestException("Email not verified. Please verify OTP first.");
        }

        // Validate terms accepted
        if (!request.isTermsAccepted()) {
            throw new BadRequestException("You must accept the terms and conditions");
        }

        // Check if already applied
        if (applicationRepository.findByUser(user).isPresent()) {
            throw new BadRequestException("You have already applied.  Check your application status.");
        }

        // Create application
        BidderApplication application = new BidderApplication();
        application. setUser(user);
        application.setPhoneNumber(request.getPhoneNumber());
        application. setStatus(Status.PENDING);
        application.setReason(request.getReason());
        application.setPreferredStallCategory(request.getPreferredStallCategory());
        application. setAppliedAt(new Timestamp(System. currentTimeMillis()));

        applicationRepository.save(application);

        // Build response
        BidderApplicationResponse response = convertToResponse(application);
        response.setStudentEmail(request.getStudentEmail());
        response.setStudentName(request. getStudentName());

        return ResponseEntity. ok(response);
    }

    @Override
    public ResponseEntity<String> approveApplication(Long applicationId) {
        BidderApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (application.getStatus() != Status.PENDING) {
            throw new BadRequestException("Application already processed.  Current status: " + application.getStatus());
        }

        // Update application status
        application.setStatus(Status.APPROVED);
        application.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        applicationRepository.save(application);

        // Update user role to BIDDER
        User user = application.getUser();
        user.setRole(Role.BIDDER);
        userRepository.save(user);

        // Send approval email
        try {
            emailService.sendApplicationApprovedEmail(
                    user.getStudentEmail(),
                    user.getStudentName()
            );
        } catch (Exception e) {
            System.err.println("Failed to send approval email:  " + e.getMessage());
        }

        return ResponseEntity. ok("Application approved successfully.  User is now a BIDDER.");
    }

    @Override
    public ResponseEntity<String> rejectApplication(Long applicationId) {
        return rejectApplicationWithReason(applicationId, "No specific reason provided");
    }

    public ResponseEntity<String> rejectApplicationWithReason(Long applicationId, String reason) {
        BidderApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (application.getStatus() != Status.PENDING) {
            throw new BadRequestException("Application already processed. Current status: " + application. getStatus());
        }

        // Update application status
        application.setStatus(Status.REJECTED);
        application.setReviewedAt(new Timestamp(System. currentTimeMillis()));
        application.setRejectionReason(reason);
        applicationRepository.save(application);

        // Send rejection email
        User user = application.getUser();
        try {
            emailService.sendApplicationRejectedEmail(
                    user.getStudentEmail(),
                    user.getStudentName(),
                    reason
            );
        } catch (Exception e) {
            System.err. println("Failed to send rejection email: " + e.getMessage());
        }

        return ResponseEntity.ok("Application rejected.");
    }

    @Override
    public ResponseEntity<String> updateApplicationStatus(Long applicationId, Status newStatus) {
        BidderApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        Status oldStatus = application.getStatus();

        application.setStatus(newStatus);
        application.setReviewedAt(new Timestamp(System. currentTimeMillis()));
        applicationRepository.save(application);

        // If approved, update user role
        if (newStatus == Status.APPROVED) {
            User user = application.getUser();
            user.setRole(Role.BIDDER);
            userRepository. save(user);

            try {
                emailService.sendApplicationApprovedEmail(
                        user.getStudentEmail(),
                        user.getStudentName()
                );
            } catch (Exception e) {
                System.err.println("Failed to send email:  " + e.getMessage());
            }
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
        if (studentEmail == null || studentEmail.isEmpty()) {
            return Status.NOT_APPLIED;
        }

        Optional<User> userOpt = userRepository. findByStudentEmail(studentEmail);

        if (userOpt.isEmpty()) {
            return Status. NOT_APPLIED;
        }

        return applicationRepository.findByUser(userOpt.get())
                .map(BidderApplication::getStatus)
                .orElse(Status.NOT_APPLIED);
    }

    @Override
    public List<BidderApplicationResponse> getAllApplications() {
        return applicationRepository. findAll().stream()
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
        response.setStudentName(app.getUser().getStudentName());
        response.setStudentEmail(app.getUser().getStudentEmail());
        response.setPhoneNumber(app. getPhoneNumber());
        response.setStatus(app.getStatus());
        response.setReason(app. getReason());
        response.setPreferredStallCategory(app.getPreferredStallCategory());
        response.setAppliedAt(app.getAppliedAt());
        response.setReviewedAt(app.getReviewedAt());
        response.setRejectionReason(app.getRejectionReason());
        return response;
    }
}