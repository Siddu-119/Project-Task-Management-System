package com.projectmgmt.service;

import com.projectmgmt.dto.CommentRequest;
import com.projectmgmt.dto.CommentResponse;
import com.projectmgmt.model.Comment;
import com.projectmgmt.model.Task;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskService taskService;

    private CommentResponse toResponse(Comment c) {
        CommentResponse r = new CommentResponse();
        r.setId(c.getId());
        r.setTaskId(c.getTask().getId());
        r.setUserId(c.getUser().getId());
        r.setUserName(c.getUser().getName());
        r.setText(c.getText());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }

    public List<CommentResponse> list(Long taskId) {
        taskService.getTask(taskId); // throws if not found / no access
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public CommentResponse add(Long taskId, CommentRequest req, Long userId) {
        taskService.getTask(taskId); // access check
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Comment c = new Comment();
        c.setTask(task);
        c.setUser(user);
        c.setText(req.getText().trim());
        return toResponse(commentRepository.save(c));
    }
}
