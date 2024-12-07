package com.taskscheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "assignedUser")
    private Set<Task> assignedTasks;

    public Long getId() {return id;}
    public String getEmail() {return email;}
    public String getUsername() {return username;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public Set<Task> getAssignedTasks() {return assignedTasks;}

    public void setId(Long id) {this.id = id;}
    public void setEmail(String email) {this.email = email;}
    public void setUsername(String username) {this.username =  username;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}