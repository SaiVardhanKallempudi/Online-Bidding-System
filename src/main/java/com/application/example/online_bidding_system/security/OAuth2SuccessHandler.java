package com.application.example.online_bidding_system.security;

import com.application.example.online_bidding_system.entity.User;
import com.application.example.online_bidding_system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.frontend.url:https://bidmart.me}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("OAuth2 authentication success handler called");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getAttribute("sub");

        log.info("OAuth2 user email: {}, name: {}", email, name);

        // Find user - DON'T create new user automatically
        Optional<User> userOptional = userRepository.findByStudentEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Update Google info and ALWAYS set emailVerified = true
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                log.info("✅ Setting Google ID for user: {}", email);
            }
            if (user.getProfilePicture() == null && picture != null) {
                user.setProfilePicture(picture);
                log.info("✅ Setting profile picture for user: {}", email);
            }

            //  IMPORTANT: Always set emailVerified = true for Google users
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                log.info("✅ Setting emailVerified=true for Google user: {}", email);
            }

            userRepository.save(user);

            log.info("User found and updated in database: {}", user.getStudentEmail());

            // Generate JWT token
            String token = jwtUtils.generateToken(user);
            log.info("JWT token generated successfully");

            // Create user data for frontend
            Map<String, Object> userData = new HashMap<>();
            userData.put("studentId", user.getStudentId());
            userData.put("studentName", user.getStudentName());
            userData.put("studentEmail", user.getStudentEmail());
            userData.put("role", user.getRole() != null ? user.getRole().name() : "USER");
            userData.put("profilePicture", user.getProfilePicture());
            userData.put("emailVerified", user.isEmailVerified()); //  Will be true
            userData.put("collageId", user.getCollageId());
            userData.put("department", user.getDepartment());
            userData.put("year", user.getYear());
            userData.put("phone", user.getPhone());
            userData.put("gender", user.getGender());
            userData.put("address", user.getAddress());

            String userJson = URLEncoder.encode(
                    objectMapper.writeValueAsString(userData),
                    StandardCharsets.UTF_8
            );

            //  Redirect to Angular app with token and user data
            String redirectUrl = frontendUrl + "/oauth/callback?token=" + token + "&user=" + userJson;
            log.info("Redirecting to: {}", redirectUrl);

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } else {
            //  User NOT found - redirect with error message
            log.warn("User not found for email: {}. User must sign up first.", email);

            String errorMessage = "No account found with this email. Please sign up first, then you can link your Google account.";
            String redirectUrl = frontendUrl + "/oauth/callback?error=" +
                    URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);

            log.info("Redirecting with error to: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
}