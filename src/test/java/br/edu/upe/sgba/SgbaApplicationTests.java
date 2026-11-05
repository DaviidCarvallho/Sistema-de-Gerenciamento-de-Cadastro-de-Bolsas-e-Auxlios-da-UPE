
package br.edu.upe.sgba;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Profile test usa H2 in-memory (application-test.properties) — roda sem PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class SgbaApplicationTests {

	@Test
	void contextLoads() {
	}

}
