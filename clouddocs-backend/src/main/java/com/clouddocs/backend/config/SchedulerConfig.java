package com.clouddocs.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for enabling scheduled tasks
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Spring will automatically detect @Scheduled methods in beans
}
