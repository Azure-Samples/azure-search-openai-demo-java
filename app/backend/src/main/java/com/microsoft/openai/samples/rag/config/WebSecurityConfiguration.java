package com.microsoft.openai.samples.rag.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

@EnableWebSecurity
@Configuration

public class WebSecurityConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.useAuthentication", havingValue = "true" )
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests ->
                requests
                    .requestMatchers("/api/auth_setup", "/api/config").permitAll()
                    .anyRequest().authenticated()
        )
        .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.useAuthentication", havingValue = "false" , matchIfMissing = true)
    public SecurityFilterChain securityFilterChainOff(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .headers(AbstractHttpConfigurer::disable)
        .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
        return http.build();
    }

}

