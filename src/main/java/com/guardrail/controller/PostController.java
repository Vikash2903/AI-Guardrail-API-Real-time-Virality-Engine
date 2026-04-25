package com.guardrail.controller;

import com.guardrail.dto.CommentRequest;
import com.guardrail.dto.LikeRequest;
import com.guardrail.dto.PostRequest;
import com.guardrail.entity.Comment;
import com.guardrail.entity.Post;
import com.guardrail.repository.PostRepository;
import com.guardrail.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final InteractionService interactionService;
    private final PostRepository postRepository;

    @GetMapping
    public ResponseEntity<Iterable<Post>> listPosts() {
        return ResponseEntity.ok(postRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostRequest request) {
        Post post = interactionService.createPost(request.getAuthorId(), request.getAuthorType(), request.getContent());
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long postId, @Valid @RequestBody CommentRequest request) {
        Comment comment = interactionService.addComment(
                postId, 
                request.getAuthorId(), 
                request.getAuthorType(), 
                request.getContent(), 
                request.getParentDepth()
        );
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable Long postId, @Valid @RequestBody LikeRequest request) {
        interactionService.likePost(postId, request.getAuthorId(), request.getAuthorType());
        return ResponseEntity.ok("Post liked successfully");
    }
}
