package com.example.gatewayserver.controllers;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/contact-support")  // ← change to match the fallbackUri
    public Mono<String> contactSupport() {
        return Mono.just("An error occurred. Please try again later.");
    }
}
