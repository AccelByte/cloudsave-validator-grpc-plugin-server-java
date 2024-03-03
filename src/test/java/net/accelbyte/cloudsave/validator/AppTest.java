package net.accelbyte.cloudsave.validator;

import net.accelbyte.cloudsave.validator.config.MockedAppConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = MockedAppConfiguration.class,
		properties = "spring.main.allow-bean-definition-overriding=true"
)
class AppTest {

	@Test
	void contextLoads() {

	}

}
