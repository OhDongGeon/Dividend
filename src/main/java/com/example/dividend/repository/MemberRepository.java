package com.example.dividend.repository;

import com.example.dividend.repository.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    // id 기준으로 회원정보를 찾기 위함
    Optional<MemberEntity> findByUsername(String username);

    // 회원정보가 있는지 여부 확인
    boolean existsByUsername(String username);
}
