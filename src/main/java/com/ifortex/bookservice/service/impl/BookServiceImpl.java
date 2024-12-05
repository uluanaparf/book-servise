package com.ifortex.bookservice.service.impl;

import com.ifortex.bookservice.dto.SearchCriteria;
import com.ifortex.bookservice.model.Book;
import com.ifortex.bookservice.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Long> getBooks() {
        String sql = "SELECT genre FROM (SELECT UNNEST(genre) AS genre FROM books) AS genres";

        // Получаем список жанров
        List<String> genres = jdbcTemplate.query(sql, (resultSet, rowNum) -> resultSet.getString("genre"));

        // Подсчитываем количество книг по жанрам
        Map<String, Long> bookCountByGenre = genres.stream()
                .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()));

        // Сортируем по количеству (value) в убывающем порядке
        return bookCountByGenre.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // Используем LinkedHashMap для сохранения порядка
                ));
    }



    @Override
    public List<Book> getAllByCriteria(SearchCriteria searchCriteria) {
        StringBuilder sql = new StringBuilder("SELECT * FROM books WHERE 1=1");
        boolean hasCriteria = false;
        if (searchCriteria!=null) {
            if (searchCriteria.getTitle() != null && !searchCriteria.getTitle().isEmpty()) {
                sql.append(" AND title LIKE '%").append(searchCriteria.getTitle()).append("%'");
                hasCriteria = true;
            }
            if (searchCriteria.getAuthor() != null && !searchCriteria.getAuthor().isEmpty()) {
                sql.append(" AND author LIKE '%").append(searchCriteria.getAuthor()).append("%'");
                hasCriteria = true;
            }
            if (searchCriteria.getGenre() != null && !searchCriteria.getGenre().isEmpty()) {
                sql.append(" AND '").append(searchCriteria.getGenre()).append("' = ANY(genre)");
                hasCriteria = true;
            }
            if (searchCriteria.getDescription() != null && !searchCriteria.getDescription().isEmpty()) {
                sql.append(" AND description LIKE '%").append(searchCriteria.getDescription()).append("%'");
                hasCriteria = true;
            }
            if (searchCriteria.getYear() != null) {
                sql.append(" AND EXTRACT(YEAR FROM publication_date) = ").append(searchCriteria.getYear());
                hasCriteria = true;
            }
        }
        if (!hasCriteria) {
            sql.append(" ORDER BY publication_date ASC");
        } else {
            sql.append(" ORDER BY publication_date ASC");
        }

        return jdbcTemplate.query(sql.toString(), new BookRowMapper());
    }

    private static class BookRowMapper implements RowMapper<Book> {
        @Override
        public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
            Book book = new Book();
            book.setId(rs.getLong("id"));
            book.setTitle(rs.getString("title"));
            book.setDescription(rs.getString("description"));
            book.setAuthor(rs.getString("author"));
            book.setPublicationDate(rs.getTimestamp("publication_date").toLocalDateTime());
            book.setGenres(Set.of((String[]) rs.getArray("genre").getArray()));
            return book;
        }
    }
}
