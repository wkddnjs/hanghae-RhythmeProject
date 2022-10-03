package com.example.rhythme_backend.service;

import com.example.rhythme_backend.domain.Member;
import com.example.rhythme_backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String Nickname) throws UsernameNotFoundException {
        Optional<Member> member = memberRepository.findByNickname(Nickname);
            return member
                    .map(UserDetailsImpl::new)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다"));

        }

    }