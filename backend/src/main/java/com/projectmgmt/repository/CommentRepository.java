package com.projectmgmt.repository;

import com.projectmgmt.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<Comment> findTop10ByTaskProjectIdOrderByCreatedAtDesc(Long projectId);
    @Transactional
    void deleteByTaskId(Long taskId);
    @Transactional
    void deleteByTaskProjectId(Long projectId);
}
