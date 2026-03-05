package com.application.example.online_bidding_system. security;

import com.application.example. online_bidding_system.entity.User;
import com. application.example.online_bidding_system.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta. servlet.http.HttpServletRequest;
import jakarta.servlet. http.HttpServletResponse;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org. springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security. core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework. stereotype.Component;
import org.springframework. util.StringUtils;
import org.springframework. web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {
                String email = jwtUtils. getEmailFromToken(jwt);
                String role = jwtUtils. getRoleFromToken(jwt);

                User user = userRepository. findByStudentEmail(email).orElse(null);

                if (user != null && user.isActive()) {
                    SimpleGrantedAuthority authority =
                            new SimpleGrantedAuthority("ROLE_" + role);
                    System.out.println("=== JWT DEBUG ===");
                    System.out.println("Email: " + email);
                    System.out.println("Role from token: " + role);
                    System.out.println("Authority stored: ROLE_" + role);
                    System.out.println("User active: " + user.isActive());
                    System.out.println("=================");

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    Collections. singletonList(authority)
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder. getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken. substring(7);
        }
        return null;
    }
}