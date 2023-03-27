package com.example.dividend.security;

import com.example.dividend.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenProvider {
    private static final String KEY_ROLES = "roles";
    private static final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1시간 동안 유효
    private final MemberService memberService;

    @Value("{spring.jwt.secret}")
    private String secretKey;


    // 토큰 생성(발급)
    public String generateToken(String username, List<String> roles) {
        // 사용자 권한 정보
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(KEY_ROLES, roles);

        var now = new Date();
        var expiredDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)   // 토큰 생성 시간
                    .setExpiration(expiredDate) // 토큰 만료 시간
                    .signWith(SignatureAlgorithm.HS512, this.secretKey) // 사용할 암호화 알고리즘, 비밀키
                    .compact();
    }


    // 인증정보 가져옴
    public Authentication getAuthentication(String jwt) {
        UserDetails userDetails = this.memberService.loadUserByUsername(this.getUsername(jwt));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }


    public String getUsername(String token) {
        return this.parseClaims(token).getSubject();
    }


    public boolean validateToken(String token) {
        // 토큰의 값이 빈 값이라면
        if (!StringUtils.hasText(token)) {
            return false;
        }

        var claims = this.parseClaims(token);
        // 토큰의 만료시간이 현재시간과 보다 이전인지 확인
        return !claims.getExpiration().before(new Date());
    }


    // 토큰이 유효한지
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
