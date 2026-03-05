package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.entity.AuthProvider;
import com.application.example.online_bidding_system.entity.Role;
import com.application.example.online_bidding_system.entity.User;
import com.application.example.online_bidding_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationTriggerService notificationTrigger; // ✅ ADDED

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String picture  = oAuth2User.getAttribute("picture");

        Optional<User> existingUser = userRepository.findByStudentEmail(email);

        if (existingUser.isPresent()) {
            // Returning user — just update profile
            User user = existingUser.get();
            if (user.getAuthProvider() == AuthProvider.LOCAL && user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            user.setProfilePicture(picture);
            user.setLastLogin(new Timestamp(System.currentTimeMillis()));
            user.setEmailVerified(true);
            userRepository.save(user);
            System.out.println("✅ Google login (returning user): " + email);

        } else {
            // ✅ Brand new Google user
            User newUser = new User();
            newUser.setStudentEmail(email);
            newUser.setStudentName(name);
            newUser.setGoogleId(googleId);
            newUser.setProfilePicture(picture);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setRole(Role.USER);
            newUser.setEmailVerified(true);
            newUser.setActive(true);
            newUser.setLastLogin(new Timestamp(System.currentTimeMillis()));
            userRepository.save(newUser);

            // ✅ Welcome notification for new Google signup
            notificationTrigger.notifyWelcome(newUser.getStudentId(), newUser.getStudentName());
            System.out.println("✅ New Google user registered + welcome notification sent: " + email);
        }

        return oAuth2User;
    }
}