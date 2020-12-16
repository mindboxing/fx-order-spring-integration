package jt.skunkworks.dataflow.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties
public class AppConfigProps {
    @NonNull
    @Value("${fundService.url:http://localhost:8091}")
    private String fundServiceUrl;

}
