package rs.casio.sales.repository;

import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class RuntimeDatabaseInitializer {
    @Bean
    DataSource dataSource(@Value("${spring.datasource.url}") String url,
                          @Value("${app.copy-reference-database:true}") boolean copyReference) throws Exception {
        Path runtime = Path.of("data", "baza_proizvoda.db");
        if (copyReference && Files.notExists(runtime)) {
            Files.createDirectories(runtime.getParent());
            try (var input = new ClassPathResource("data/baza_proizvoda.db").getInputStream()) {
                Files.copy(input, runtime);
            }
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl(url);
        return dataSource;
    }
}
