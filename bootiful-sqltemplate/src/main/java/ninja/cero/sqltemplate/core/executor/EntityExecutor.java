package ninja.cero.sqltemplate.core.executor;

import ninja.cero.sqltemplate.core.mapper.MapperBuilder;
import ninja.cero.sqltemplate.core.parameter.ParamBuilder;
import ninja.cero.sqltemplate.core.stream.StreamResultSetExtractor;
import ninja.cero.sqltemplate.core.template.TemplateEngine;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class EntityExecutor implements QueryExecutor {
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    private ParamBuilder paramBuilder;
    private MapperBuilder mapperBuilder;
    private TemplateEngine templateEngine;
    private String template;
    private Object entity;

    public EntityExecutor(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate, ParamBuilder paramBuilder, MapperBuilder mapperBuilder, TemplateEngine templateEngine, String template, Object entity) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.paramBuilder = paramBuilder;
        this.mapperBuilder = mapperBuilder;
        this.templateEngine = templateEngine;
        this.template = template;
        this.entity = entity;
    }

    @Override
    public <T> T forObject(Class<T> clazz) {
        List<T> list = forList(clazz);
        return DataAccessUtils.singleResult(list);
    }

    @Override
    public <T> List<T> forList(Class<T> clazz) {
        try {
            String sql = templateEngine.get(template, entity);
            return namedJdbcTemplate.query(sql, paramBuilder.byBean(entity), mapperBuilder.mapper(clazz));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T, U> U forStream(Class<T> clazz, Function<? super Stream<T>, U> handler) {
        try {
            String sql = templateEngine.get(template, entity);
            SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
            ResultSetExtractor<U> extractor = new StreamResultSetExtractor<>(sql, mapperBuilder.mapper(clazz), handler, excTranslator);
            return namedJdbcTemplate.query(sql, paramBuilder.byBean(entity), extractor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Map<String, Object> forMap() {
        List<Map<String, Object>> list = forList();
        return DataAccessUtils.singleResult(list);
    }

    @Override
    public List<Map<String, Object>> forList() {
        try {
            String sql = templateEngine.get(template, entity);
            return namedJdbcTemplate.queryForList(sql, paramBuilder.byBean(entity));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <U> U forStream(Function<? super Stream<Map<String, Object>>, U> handler) {
        try {
            String sql = templateEngine.get(template, entity);
            SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
            // TODO: can it work with zoneId?
            ResultSetExtractor<U> extractor = new StreamResultSetExtractor<>(sql, new ColumnMapRowMapper(), handler, excTranslator);
            return namedJdbcTemplate.query(sql, paramBuilder.byBean(entity), extractor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
