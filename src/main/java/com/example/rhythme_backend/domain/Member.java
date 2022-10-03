package com.example.rhythme_backend.domain;

import com.example.rhythme_backend.dto.requestDto.profile.ModifyProfileRequestDto;
import com.example.rhythme_backend.util.Timestamped;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.persistence.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@SQLDelete(sql = "UPDATE member SET delete_check = 'Y' where id = ?")
public class Member extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    @Column
    private String name;
    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(unique = true)
    private Long kakaoId;

    @Column(unique = true)
    private String googleId;
    @Column
    private String imageUrl;
    @Column
    private String introduce;
    @Column
    private Long followers;
    @Column
    private String deleteCheck;

    public boolean validatePassword(PasswordEncoder passwordEncoder, String password) {
        return passwordEncoder.matches(password, this.password);
    }

    public void updateFollowers(Long followers) {
        this.followers = followers;
    }

    public void update(ModifyProfileRequestDto modifyProfileRequestDto) {
        this.nickname = modifyProfileRequestDto.getNickname();
        this.imageUrl = modifyProfileRequestDto.getImageUrl();
        this.introduce = modifyProfileRequestDto.getIntroduce();
    }

    public void save(ModifyProfileRequestDto modifyProfileRequestDto) {
        this.nickname = modifyProfileRequestDto.getNickname();
        this.imageUrl = modifyProfileRequestDto.getImageUrl();
        this.introduce = modifyProfileRequestDto.getIntroduce();
    }

}
