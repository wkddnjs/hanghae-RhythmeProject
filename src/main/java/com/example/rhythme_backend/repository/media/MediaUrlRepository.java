package com.example.rhythme_backend.repository.media;

import com.example.rhythme_backend.domain.media.MediaUrl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaUrlRepository extends JpaRepository<MediaUrl,Long> {
}
