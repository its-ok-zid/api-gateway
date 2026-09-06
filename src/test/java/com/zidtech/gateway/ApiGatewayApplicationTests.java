package com.zidtech.gateway;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class ApiGatewayApplicationTests {

    @Test
    void contextLoadsAndMainMethodRuns() {
        ApiGatewayApplication app = new ApiGatewayApplication();
        assertNotNull(app);

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            ApiGatewayApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(ApiGatewayApplication.class, new String[]{}));
        }
    }
}