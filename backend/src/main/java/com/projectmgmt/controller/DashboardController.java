package com.projectmgmt.controller;

import com.projectmgmt.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/api/dashboard")
    public Map<String, Object> dashboard() {
        return dashboardService.dashboard();
    }

    @GetMapping("/api/reports")
    public Map<String, Object> reports() {
        return dashboardService.reports();
    }
}
