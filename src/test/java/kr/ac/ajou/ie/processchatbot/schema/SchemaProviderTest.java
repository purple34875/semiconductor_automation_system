package kr.ac.ajou.ie.processchatbot.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SchemaProviderTest {

	@Test
	void shouldLoadSchemaMetadataFromYaml() {
		SchemaProvider schemaProvider = new SchemaProvider();
		schemaProvider.init();

		assertThat(schemaProvider.getSchemaMetadata().tables()).hasSize(3);
		assertThat(schemaProvider.buildSchemaDescription()).contains("Process_Log");
		assertThat(schemaProvider.buildSchemaDescription()).contains("작업자 이름");
	}
}
