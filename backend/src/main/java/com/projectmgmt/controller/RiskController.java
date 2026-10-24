package com.projectmgmt.controller;

import com.projectmgmt.logic.RiskResult;
import com.projectmgmt.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class RiskController {

    @Autowired
    private RiskService riskService;

    @GetMapping("/api/projects/{id}/risk")
    public RiskResult projectRisk(@PathVariable Long id) {
        return riskService.projectRisk(id);
    }

    @GetMapping("/api/tasks/{id}/risk")
    public RiskResult taskRisk(@PathVariable Long id) {
        return riskService.taskRisk(id);
    }
}
