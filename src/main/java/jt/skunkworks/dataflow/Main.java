package jt.skunkworks.dataflow;

import jt.skunkworks.dataflow.config.AppConfigProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppConfigProps.class)
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

}
