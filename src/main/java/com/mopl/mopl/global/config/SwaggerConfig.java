package com.mopl.mopl.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "Mopl API 명세서",
                description = "Mopl API 명세서입니다.",
                version = "v0.1"
        )
)
@Configuration
public class SwaggerConfig {

}
