package com.guardrail.controller;

import com.guardrail.entity.Bot;
import com.guardrail.entity.User;
import com.guardrail.repository.BotRepository;
import com.guardrail.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {

    private final UserRepository userRepository;
    private final BotRepository botRepository;

    @PostMapping
    public String seed() {
        if (userRepository.count() == 0) {
            User user = new User(null, "human_user", true);
            userRepository.save(user);
        }
        if (botRepository.count() == 0) {
            Bot bot = new Bot(null, "GuardBot", "A helpful bot");
            botRepository.save(bot);
        }
        return "Seed data created";
    }
}
