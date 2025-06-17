package com.sonsminpark.auratalkback.global.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class WebSocketUser implements Principal {
    private final String email;
    private final Long userId;

    @Override
    public String getName() {
        return email;
    }
}