package com.aoms.aomsbackend;

import com.aoms.aomsbackend.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestSecurityConfig.class)
class AomsBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
