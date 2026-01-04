package com.application.example.online_bidding_system. config;

import com.application.example. online_bidding_system.entity.AuthProvider;
import com.application.example. online_bidding_system.entity.Role;
import com. application.example.online_bidding_system.entity.User;
import com.application.example.online_bidding_system.repository. UserRepository;
import org.springframework.beans.factory.annotation. Autowired;
import org.springframework. beans.factory.annotation. Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.default. email: admin@college.edu}")
    private String adminEmail;

    @Value("${admin. default.password:Admin@123}")
    private String adminPassword;

    @Value("${admin.default.name:System Administrator}")
    private String adminName;

    @Override
    public void run(String...  args) {
        if (userRepository.findByStudentEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setStudentName(adminName);
            admin.setStudentEmail(adminEmail);
            admin. setPassword(passwordEncoder.encode(adminPassword));
            admin. setRole(Role.ADMIN);
            admin. setAuthProvider(AuthProvider.LOCAL);
            admin.setEmailVerified(true);
            admin.setActive(true);

            userRepository.save(admin);

            System.out. println("========================================");
            System. out.println("  DEFAULT ADMIN CREATED SUCCESSFULLY!");
            System.out.println("  Email: " + adminEmail);
            System.out.println("  Password: " + adminPassword);
            System.out.println("  Role:  ADMIN");
            System.out.println("========================================");
        }
    }
}