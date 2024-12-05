package com.ifortex.bookservice.service.impl;

import com.ifortex.bookservice.model.Member;
import com.ifortex.bookservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Primary
@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Member findMember() {
        String sql = """
            SELECT m.id, m.name, m.membership_date 
            FROM members m
            JOIN member_books mb ON m.id = mb.member_id
            JOIN books b ON mb.book_id = b.id
            WHERE 'Romance' = ANY(b.genre)
            ORDER BY b.publication_date ASC, m.membership_date DESC
            LIMIT 1
        """;

        return jdbcTemplate.queryForObject(sql, new MemberRowMapper());
    }

    @Override
    public List<Member> findMembers() {
        String sql = """
            SELECT m.id, m.name, m.membership_date 
            FROM members m
            LEFT JOIN member_books mb ON m.id = mb.member_id
            WHERE EXTRACT(YEAR FROM m.membership_date) = 2023 AND mb.member_id IS NULL
        """;

        return jdbcTemplate.query(sql, new MemberRowMapper());
    }

    private static class MemberRowMapper implements RowMapper<Member> {
        @Override
        public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
            Member member = new Member();
            member.setId(rs.getLong("id"));
            member.setName(rs.getString("name"));
            member.setMembershipDate(rs.getTimestamp("membership_date").toLocalDateTime());
            return member;
        }
    }
}
