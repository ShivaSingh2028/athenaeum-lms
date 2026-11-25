package com.college.library.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts cloud DATABASE_URL (standard on Render, Railway, Heroku)
 * and automatically maps postgres:// or postgresql:// to JDBC DataSource and Flyway.
 */
public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(?:postgres|postgresql)://(?:([^:]+):([^@]+)@)?([^:/]+)(?::([0-9]+))?/(.+)$"
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 15;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            databaseUrl = System.getenv("DATABASE_URL");
        }

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            Matcher matcher = URL_PATTERN.matcher(databaseUrl.trim());
            if (matcher.matches()) {
                String username = matcher.group(1);
                String password = matcher.group(2);
                String host = matcher.group(3);
                String port = matcher.group(4) != null ? matcher.group(4) : "5432";
                String dbAndQuery = matcher.group(5);

                String db = dbAndQuery.contains("?") ? dbAndQuery.substring(0, dbAndQuery.indexOf('?')) : dbAndQuery;
                String query = dbAndQuery.contains("?") ? dbAndQuery.substring(dbAndQuery.indexOf('?') + 1) : "sslmode=require";
                if (!query.contains("sslmode")) {
                    query = query + (query.isEmpty() ? "" : "&") + "sslmode=require";
                }

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?" + query;

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl);
                props.put("spring.flyway.url", jdbcUrl);

                if (username != null) {
                    props.put("spring.datasource.username", username);
                    props.put("spring.flyway.user", username);
                }
                if (password != null) {
                    props.put("spring.datasource.password", password);
                    props.put("spring.flyway.password", password);
                }

                environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlProperties", props));
            }
        }
    }
}
