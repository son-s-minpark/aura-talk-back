package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.RandomChatStartRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.RandomChatMatchResponseDto;

public interface RandomChatService {
    RandomChatMatchResponseDto startRandomChat(Long userId, RandomChatStartRequestDto requestDto);
}