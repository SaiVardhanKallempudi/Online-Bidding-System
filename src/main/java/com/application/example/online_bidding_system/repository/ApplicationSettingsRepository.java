package com.application.example.online_bidding_system. repository;

import com.application.example. online_bidding_system.entity.ApplicationSettings;
import org.springframework.data. jpa.repository. JpaRepository;
import org.springframework. stereotype.Repository;
import java.util. Optional;

@Repository
public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings, Long> {
    Optional<ApplicationSettings> findTopByOrderByIdDesc();
}