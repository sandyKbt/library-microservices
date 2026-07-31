package com.library.bookservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false")
class BookServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
