package com.sonsminpark.auratalkback.domain.user.service;

public interface EmailService {
    void sendVerificationEmail(String email, String token);

    String generateVerificationToken(String email);

    boolean validateVerificationToken(String email, String token);
}