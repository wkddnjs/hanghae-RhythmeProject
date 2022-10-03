package com.example.rhythme_backend.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChatRoomListDto {
    private List<ChatRoomResponseDto> messageDto;
    private boolean isOwner;

    public ChatRoomListDto(List<ChatRoomResponseDto> chatRoomResponseDtoList, Boolean isOwner) {
        this.messageDto = chatRoomResponseDtoList;
        this.isOwner = isOwner;
    }
}
