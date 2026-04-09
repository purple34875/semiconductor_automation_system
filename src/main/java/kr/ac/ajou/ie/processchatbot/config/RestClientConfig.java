package kr.ac.ajou.ie.processchatbot.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	RestClient ollamaHealthRestClient(
		RestClient.Builder builder,
		@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl
	) {
		var requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(5));
		requestFactory.setReadTimeout(Duration.ofSeconds(10));

		return builder
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
	}
}
