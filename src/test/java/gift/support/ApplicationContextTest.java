package gift.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationContextTest extends IntegrationTestSupport {
    @Test
    @DisplayName("스프링 애플리케이션 컨텍스트가 정상 로드된다")
    void contextLoads() {
        // when & then
    }

    @Test
    @DisplayName("Flyway 마이그레이션과 기본 데이터가 정상 적용된다")
    void flywayAppliesMigrationsAndDefaultData() {
        // when
        Integer appliedMigrationCount = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true",
            Integer.class
        );
        Integer productCount = jdbcTemplate.queryForObject("select count(*) from product", Integer.class);
        String firstProductName = jdbcTemplate.queryForObject(
            "select name from product where id = 1",
            String.class
        );

        // then
        assertThat(appliedMigrationCount).isEqualTo(2);
        assertThat(productCount).isEqualTo(6);
        assertThat(firstProductName).isEqualTo("맥북 프로 16인치");
    }
}
