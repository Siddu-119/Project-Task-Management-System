package com.projectmgmt.repository;

import com.projectmgmt.model.Task;
import com.projectmgmt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedTo(User user);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssignedToId(Long userId);
    List<Task> findByProjectIdIn(java.util.Collection<Long> projectIds);
    @Transactional
    void deleteByProjectId(Long projectId);
}
