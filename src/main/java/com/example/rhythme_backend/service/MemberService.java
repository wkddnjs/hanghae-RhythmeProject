package com.example.rhythme_backend.service;

import com.example.rhythme_backend.domain.HashTag;
import com.example.rhythme_backend.domain.Member;
import com.example.rhythme_backend.domain.Message;
import com.example.rhythme_backend.dto.TokenDto;
import com.example.rhythme_backend.dto.requestDto.member.*;
import com.example.rhythme_backend.dto.responseDto.ResignResponseDto;
import com.example.rhythme_backend.exception.CustomException;
import com.example.rhythme_backend.exception.ErrorCode;
import com.example.rhythme_backend.jwt.TokenProvider;
import com.example.rhythme_backend.repository.HashTagRepository;
import com.example.rhythme_backend.repository.MemberRepository;
import com.example.rhythme_backend.repository.RefreshTokenRepository;
import com.example.rhythme_backend.service.googleLogin.Constant;
import com.example.rhythme_backend.service.googleLogin.GoogleOauth;
import com.example.rhythme_backend.service.kakaoLogin.KakaoOauth;
import com.example.rhythme_backend.util.RefreshToken;
import com.example.rhythme_backend.util.Validation;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleOauth googleOauth;
    private final HttpServletResponse response;
    private final KakaoOauth kakaoOauth;
    private final HashTagRepository hashTagRepository;
    private final Validation validation;

    //============ ????????????
    @Transactional
    public ResponseEntity<?> signupMember(SignupRequestDto requestDto) {
        if (memberRepository.existsByEmail(requestDto.getEmail())) {
            return new ResponseEntity<>(Message.fail("DUPLICATED_EMAIL", "????????? ??????????????????."), HttpStatus.BAD_REQUEST);
        }
        String defaultIntro = "???????????? ???????????? ??????????????????!";
        Member member = Member.builder()
                .deleteCheck("N")
                .email(requestDto.getEmail())
                .imageUrl(requestDto.getImgUrl())
                .nickname(requestDto.getNickname())
                .followers(0L)
                .introduce(defaultIntro)
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .build();
        memberRepository.save(member);
        hashTagSave(requestDto.getHashtag(), member);
        return new ResponseEntity<>(Message.success("??????????????? ??????????????????."), HttpStatus.OK);
    }

    //============ ????????? ?????? ??????
    @Transactional
    public ResponseEntity<?> emailCheck(EmailCheckRequestDto requestDto) {
        if (memberRepository.existsByEmail(requestDto.getEmail())) {
            return new ResponseEntity<>(Message.fail("DUPLICATED_EMAIL", "?????? ???????????? ??????????????????."), HttpStatus.OK);
        }
        return new ResponseEntity<>(Message.success("?????? ????????? ??????????????????."), HttpStatus.OK);
        }

    //============ ????????? ?????? ??????
    @Transactional
    public ResponseEntity<?> nicknameCheck(NicknameCheckRequestDto requestDto) {
        if (memberRepository.existsByNickname(requestDto.getNickname())) {
            return new ResponseEntity<>(Message.fail("DUPLICATED_NICKNAME","?????? ???????????? ??????????????????."),HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(Message.success("?????? ????????? ??????????????????."),HttpStatus.OK);
    }

    //============ ????????? ??????
    @Transactional
    public ResponseEntity<?> loginMember(LoginRequestDto requestDto, HttpServletResponse response) {
        Member member = validation.getPresentEmail(requestDto.getEmail());
        if (!memberRepository.existsByEmail(requestDto.getEmail())) {
            return new ResponseEntity<>(Message.fail("EMAIL_NOT_FOUND","???????????? ?????? ??????????????????."),HttpStatus.NOT_FOUND);
        }
        if (!member.validatePassword(passwordEncoder, requestDto.getPassword())) {
            return new ResponseEntity<>(Message.fail("PASSWORD_NOT_FOUND","??????????????? ?????? ??????????????????."),HttpStatus.NOT_FOUND);
        }
        TokenDto tokenDto = tokenProvider.generateTokenDto(member);
        validation.tokenToHeaders(tokenDto,response);
        return new ResponseEntity<>(Message.success("??????????????? ????????? ???????????????."),HttpStatus.OK);
    }

    //============ ???????????? ??????
    @Transactional
    public ResponseEntity<?> resignMember(ResignRequestDto requestDto, HttpServletRequest request) {
        validation.validateMemberToAccess(request);
        Member member = memberRepository.findByNickname(requestDto.getEmail()).orElseThrow(
                ()->new IllegalArgumentException("NICKNAME_NOT_FOUND"));
        RefreshToken deleteToken = validation.getDeleteToken(member);
        String deleteEmail = requestDto.getEmail();
        Member deleteMember = validation.getPresentEmail(deleteEmail);
        Long deleteMemberId = deleteMember.getId();
        Member resignMember = validation.getDeleteMember(deleteMemberId);
        refreshTokenRepository.delete(deleteToken);
        memberRepository.delete(resignMember);
        return new ResponseEntity<>(Message.success(
                ResignResponseDto.builder()
                        .email(deleteMember.getEmail())
                        .build()
        ),HttpStatus.OK);
    }

    //============ ???????????? ??????
    public ResponseEntity<?> logoutMember(LogoutRequestDto requestDto, HttpServletRequest request) {
        validation.validateMemberToAccess(request);
        Member member = memberRepository.findByNickname(requestDto.getNickname()).orElseThrow(
                ()-> new IllegalArgumentException("NICKNAME_NOT_FOUND"));
        String logoutNickname = member.getNickname();
        Member logoutMember = validation.getPresentNickname(logoutNickname);
        Long logoutMemberId = logoutMember.getId();
        Member logout = validation.getDeleteMember(logoutMemberId);
        tokenProvider.deleteRefreshToken(logout);
        return new ResponseEntity<>(Message.success("???????????? ???????????????."),HttpStatus.OK);
    }

    //============ ????????? ?????????
    @Transactional
    public TokenDto kakaoLogin(String code) throws JsonProcessingException {
        String accessToken = kakaoOauth.getAccessToken(code);
        // 2. ???????????? ????????? API ??????
        KakaoUserInfoDto kakaoUserInfo = kakaoOauth.getKakaoUserInfo(accessToken);
        // DB ??? ????????? KakaoId ??? ????????? ??????
        Long kakaoId = kakaoUserInfo.getKakaoid();
        Member kakaoUser = memberRepository.findByKakaoId(kakaoId)
                .orElse(null);
        if (kakaoUser == null) {
            // ????????????
            String defaultIntro = "???????????? ???????????? ??????????????????!";
            Random random = new Random();
            String kakaoDefaultName = "KAKAO" + random.nextInt(1000000000);
            String name = kakaoUserInfo.getName();
            String password = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);
            //????????? ?????????
            String email = kakaoUserInfo.getEmail();
            kakaoUser = Member.builder()
                    .deleteCheck("N")
                    .kakaoId(kakaoId)
                    .introduce(defaultIntro)
                    .followers(0L)
                    .nickname(kakaoDefaultName)
                    .email(email)
                    .name(name)
                    .password(encodedPassword)
                    .build();
            memberRepository.save(kakaoUser);
        }
        // 4. ?????? kakao????????? ??????
        UserDetails kakaouserDetails = new UserDetailsImpl(kakaoUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(kakaouserDetails, null, kakaouserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Member member = validation.getPresentEmail(kakaoUser.getEmail());
        return tokenProvider.generateTokenDto(member);

    }

    //google
    @Transactional
    public void request(Constant.SocialLoginType socialLoginType) throws IOException {
        String redirectURL;
        if (socialLoginType == Constant.SocialLoginType.GOOGLE) {
            redirectURL = googleOauth.getOauthRedirectURL();
        } else {
            throw new IllegalArgumentException("??? ??? ?????? ?????? ????????? ???????????????.");
        }
        response.sendRedirect(redirectURL);
    }


    @Transactional
    public TokenDto oAuthLogin(String code) throws IOException {
                //????????? ????????? ????????? ?????? ????????? ????????? ?????? ??????????????? ?????????
                ResponseEntity<String> accessTokenResponse = googleOauth.requestAccessToken(code);
                //?????? ????????? JSON???????????? ?????? ????????????, ?????? deserialization?????? ?????? ????????? ?????? ?????????.
                GoogleOAuthTokenDto oAuthTokenDto = googleOauth.getAccessToken(accessTokenResponse);
                //????????? ????????? ?????? ????????? ?????? ????????? ????????? ????????? ????????? ?????? ?????? ????????? ????????????.
                GoogleUserInfoDto googleUser=googleOauth.requestUserInfo(oAuthTokenDto);
                String googleId = googleUser.getGoogleId();
                Member googleLoginUser = memberRepository.findByGoogleId(googleId)
                        .orElse(null);
                if (googleLoginUser == null) {
                    // ????????????
                    String defaultIntro = "???????????? ???????????? ??????????????????!";
                    Random random = new Random();
                    String name = googleUser.getName();
                    String googleDefaultName = "google" + random.nextInt(1000000000);
                    String password = UUID.randomUUID().toString();
                    String encodedPassword = passwordEncoder.encode(password);
                    String email = googleUser.getEmail();
                    googleLoginUser = Member.builder()
                            .deleteCheck("N")
                            .introduce(defaultIntro)
                            .googleId(googleId)
                            .followers(0L)
                            .nickname(googleDefaultName)
                            .email(email)
                            .name(name)
                            .password(encodedPassword)
                            .build();
                    memberRepository.save(googleLoginUser);
                }
                // 4. ?????? ??????????????? ??????
                UserDetails googleUserDetails = new UserDetailsImpl(googleLoginUser);
                Authentication authentication = new UsernamePasswordAuthenticationToken(googleUserDetails, null, googleUserDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                Member googleMember = validation.getPresentEmail(googleLoginUser.getEmail());
                return tokenProvider.generateTokenDto(googleMember);

    }

    //============ ?????????????????? ??????
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        tokenProvider.validateToken(request.getHeader("Refresh-Token"));
        Member requestingMember = validation.validateMemberToRefresh(request);
        long accessTokenExpire = Long.parseLong(request.getHeader("Access-Token-Expire-Time"));
        long now = (new Date().getTime());

        if (now>accessTokenExpire){
            tokenProvider.deleteRefreshToken(requestingMember);
            throw new CustomException(ErrorCode.INVALID_TOKEN);}

        RefreshToken refreshTokenConfirm = refreshTokenRepository.findByMember(requestingMember).orElse(null);
        if (refreshTokenConfirm == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_IS_EXPIRED);
        }
        if (Objects.equals(refreshTokenConfirm.getValue(), request.getHeader("Refresh-Token"))) {
            TokenDto tokenDto = tokenProvider.generateAccessTokenDto(requestingMember);
            validation.accessTokenToHeaders(tokenDto, response);
            return new ResponseEntity<>(Message.success("ACCESS_TOKEN_REISSUE"), HttpStatus.OK);
        } else {
            tokenProvider.deleteRefreshToken(requestingMember);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public void hashTagSave(List<String> hashtag,Member member){
        for(String tag : hashtag){
            hashTagRepository.save(
                    HashTag.builder()
                            .member(member)
                            .hashtag(tag)
                            .build());
        }
    }

}
