package com.application.example. online_bidding_system.security;

import com.application. example.online_bidding_system.entity.User;
import com.application.example.online_bidding_system.repository. UserRepository;
import jakarta.servlet.ServletException;
import jakarta. servlet.http.HttpServletRequest;
import jakarta.servlet. http.HttpServletResponse;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.beans.factory.annotation. Value;
import org.springframework.security. core.Authentication;
import org.springframework. security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication. SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework. web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        String token = jwtUtils. generateToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("role", user.getRole().name())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}