package com.guardrail.service;

import com.guardrail.entity.*;
import com.guardrail.exception.GuardrailException;
import com.guardrail.repository.BotRepository;
import com.guardrail.repository.CommentRepository;
import com.guardrail.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;


@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final BotRepository botRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;


    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_DEPTH = 20;
    private static final int COOLDOWN_MINUTES = 10;

    @Transactional
    public Post createPost(Long authorId, AuthorType authorType, String content) {
        Post post = Post.builder()
                .authorId(authorId)
                .authorType(authorType)
                .content(content)
                .build();
        return postRepository.save(post);
    }

    @Transactional
    public Comment addComment(Long postId, Long authorId, AuthorType authorType, String content, int parentDepth) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GuardrailException("Post not found", 404));

        int newDepth = parentDepth + 1;

        // Vertical Cap Guardrail
        if (newDepth > MAX_DEPTH) {
            throw new GuardrailException("Comment thread too deep (Max 20 levels)", 400);
        }

        if (authorType == AuthorType.BOT) {
            // Horizontal Cap Guardrail (Atomic)
            String botCountKey = "post:" + postId + ":bot_count";
            Long count = redisTemplate.opsForValue().increment(botCountKey);
            if (count != null && count > MAX_BOT_REPLIES) {
                redisTemplate.opsForValue().decrement(botCountKey);
                throw new GuardrailException("Rate limit exceeded: A post cannot have more than 100 bot replies", 429);
            }

            // Cooldown Cap Guardrail
            String cooldownKey = "cooldown:bot_" + authorId + ":human_" + post.getAuthorId();
            if (post.getAuthorType() == AuthorType.USER) {
                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "active", java.time.Duration.ofMinutes(COOLDOWN_MINUTES));
                if (Boolean.FALSE.equals(isNew)) {
                    redisTemplate.opsForValue().decrement(botCountKey);
                    throw new GuardrailException("Bot cooldown active for this user (10 minutes)", 429);
                }
            }

            // Virality Score Update
            updateViralityScore(postId, 1);
            
            // Notification Logic
            if (post.getAuthorType() == AuthorType.USER) {
                Bot bot = botRepository.findById(authorId).orElse(null);
                String botName = (bot != null) ? bot.getName() : "Unknown Bot";
                notificationService.processBotNotification(post.getAuthorId(), botName + " replied to your post");
            }
        } else {
            // Human Comment = +50 Points
            updateViralityScore(postId, 50);
        }

        Comment comment = Comment.builder()
                .post(post)
                .authorId(authorId)
                .authorType(authorType)
                .content(content)
                .depthLevel(newDepth)
                .build();

        return commentRepository.save(comment);
    }

    @Transactional
    public void likePost(Long postId, Long authorId, AuthorType authorType) {
        if (!postRepository.existsById(postId)) {
            throw new GuardrailException("Post not found", 404);
        }

        if (authorType == AuthorType.USER) {
            updateViralityScore(postId, 20);
        }
    }

    private void updateViralityScore(Long postId, int points) {
        String viralityKey = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(viralityKey, points);
    }

}
