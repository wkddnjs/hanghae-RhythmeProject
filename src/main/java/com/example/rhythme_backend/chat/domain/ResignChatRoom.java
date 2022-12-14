package com.example.rhythme_backend.chat.domain;

import com.example.rhythme_backend.chat.domain.chat.ChatRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ResignChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private String username;

    public ResignChatRoom(ChatRoom chatRoom) {
        this.roomId = chatRoom.getRoomId();
        this.username = chatRoom.getUsername();
    }
}
