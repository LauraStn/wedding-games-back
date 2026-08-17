package com.weddinggames.backend.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weddingGamesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wedding Games API")
                        .description(
                                "API du socle applicatif pour l'application privee d'animations de mariage "
                                        + "(evenement, participants, invitations, salon d'attente, exclusions).")
                        .version("v1")
                        .contact(new Contact().name("Organisation du mariage")));
    }
}
