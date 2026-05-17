package gift;

import gift.auth.support.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
// Avoid reusing a Spring DataSource after a previous test class has stopped its MySQL container.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class IntegrationTestSupport {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtProvider jwtProvider;

    protected String authorization(String email) {
        return "Bearer " + jwtProvider.createToken(email);
    }
}
