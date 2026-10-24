package com.projectmgmt.controller;

import com.projectmgmt.dto.*;
import com.projectmgmt.service.ProjectMemberService;
import com.projectmgmt.service.ProjectService;
import com.projectmgmt.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:3000")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectMemberService memberService;
    @Autowired
    private TaskService taskService;

    @GetMapping
    public List<ProjectResponse> getAllProjects(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) String status) {
        return projectService.search(search, status);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id,
                                                         @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    // ----- team members -----
    @GetMapping("/{id}/members")
    public List<MemberResponse> getMembers(@PathVariable Long id) {
        return memberService.list(id);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> addMember(@PathVariable Long id,
                                                          @Valid @RequestBody MemberRequest request) {
        memberService.add(id, request.getUserId());
        return ResponseEntity.ok(memberService.list(id));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        memberService.remove(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/activity")
    public List<java.util.Map<String, Object>> getActivity(@PathVariable Long id) {
        return projectService.activity(id);
    }

    // ----- tasks of a project (used by the Kanban board) -----
    @GetMapping("/{id}/tasks")
    public List<TaskResponse> getProjectTasks(@PathVariable Long id) {
        return taskService.getTasksByProject(id);
    }
}
