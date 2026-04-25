package com.guardrail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
    
    private Long authorId;
    
    @Enumerated(EnumType.STRING)
    private AuthorType authorType;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private int depthLevel;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
