package com.edgareldy.springgraphqltutorial;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Smoke test verifying the Spring application context starts successfully.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SpringGraphqlTutorialApplicationTests {

	@Test
	void contextLoads() {
	}

}
