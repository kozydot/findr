package com.example.price_comparator.config;

import com.example.price_comparator.service.ScheduledTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Profile("!test")
public class ScheduledTasksConfig {

    @Autowired
    private ScheduledTasksService scheduledTasksService;

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void scheduleHourlyProductUpdate() {
        scheduledTasksService.scheduleHourlyProductUpdate();
    }
}