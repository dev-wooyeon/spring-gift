package gift;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationContextTest extends IntegrationTestSupport {
    @Test
    void contextLoads() {
    }

    @Test
    void flywayAppliesMigrationsAndDefaultData() {
        Integer appliedMigrationCount = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true",
            Integer.class
        );
        Integer productCount = jdbcTemplate.queryForObject("select count(*) from product", Integer.class);
        String firstProductName = jdbcTemplate.queryForObject(
            "select name from product where id = 1",
            String.class
        );

        assertThat(appliedMigrationCount).isEqualTo(2);
        assertThat(productCount).isEqualTo(6);
        assertThat(firstProductName).isEqualTo("맥북 프로 16인치");
    }
}
