package com.example.dividend.service;


import com.example.dividend.exception.impl.AlreadyExistUserException;
import com.example.dividend.model.Auth;
import com.example.dividend.repository.MemberRepository;
import com.example.dividend.repository.entity.MemberEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // 사용자 비밀번호 암호화

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("couldn't find user -> " + username));
    }


    // 회원가입 지원
    public MemberEntity register(Auth.SignUp member) {
        // 중복확인
        boolean exists = this.memberRepository.existsByUsername(member.getUsername());
        if(exists) {
            throw new AlreadyExistUserException();
        }
        // 비밀번호 인코딩
        member.setPassword(this.passwordEncoder.encode(member.getPassword()));
        // 저장
        var result = this.memberRepository.save(member.toEntity());
        return result;
    }


    // 로그인 검증
    public MemberEntity authenticate(Auth.SignIn member) {
        var user = this.memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 ID 입니다."));

        if(!this.passwordEncoder.matches(member.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
