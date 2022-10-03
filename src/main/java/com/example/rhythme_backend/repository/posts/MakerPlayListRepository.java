package com.example.rhythme_backend.repository.posts;


import com.example.rhythme_backend.domain.Member;
import com.example.rhythme_backend.domain.post.MakerPlayList;
import com.example.rhythme_backend.domain.post.MakerPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MakerPlayListRepository extends JpaRepository<MakerPlayList,Long> {

    List<MakerPlayList> findByMemberOrderByCreatedAtDesc(Member member);
    Long deleteAllByMember(Member member);
    Long deleteByMakerPost(MakerPost makerPost);
    Long deleteByMakerPostAndMember(MakerPost makerPost,Member member);
    Boolean existsByMakerPostAndMember(MakerPost makerPost,Member member);

}
