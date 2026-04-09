package kr.ac.ajou.ie.processchatbot.schema;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Slf4j
@Component
public class SchemaProvider {

	private static final String INTERPRETATION_RULES = """
	중요한 해석 규칙
	- 실제 작업 기록은 Process_Log에 저장된다
	- 작업자 이름은 Process_Log에 직접 저장되지 않고 Operator_Master와 조인해서 확인한다
	- 공정명은 Process_Log에 직접 저장되지 않고 Process_Master와 조인해서 확인한다
	- 사용자가 '작업 기록', '로그', '이력', '작업 내역'이라고 말하면 보통 Process_Log를 의미한다
	- 사용자가 '작업자', '담당자', '누가 했는지'라고 말하면 보통 operator_name을 의미한다
	- 사용자가 '공정', '공정명'이라고 말하면 보통 process_name을 의미한다
	- 사용자가 'cassette'라고 말하면 cassette_id를 의미한다
	- 사용자가 '날짜', '오늘', '어제', '최근', '기간'이라고 말하면 보통 work_date 또는 날짜 범위 조건을 의미한다
	- 사용자가 '비고', '특이사항', '메모'라고 말하면 remarks를 의미한다
	""";

	private static final String RESPONSE_GUIDELINES = """
	조회 원칙
	- 존재하지 않는 테이블이나 컬럼을 만들지 않는다
	- 제공된 스키마와 조회 결과만 사용한다
	- 데이터가 없으면 추측하지 말고 없다고 분명히 말한다
	- 작업자 이름이나 공정명을 설명할 때는 필요 시 조인된 결과를 기준으로 설명한다

	답변 방식
	- 답변은 한국어로 짧고 명확하게 작성한다
	- 여러 건이면 날짜, 작업자명, 공정명, cassette_id를 우선적으로 정리한다
	- 조회 결과가 없으면 '조건에 맞는 데이터가 없습니다.'라고 안내한다
	- 질문이 모호하면 필요한 조건(예: 작업자명, 공정명, 날짜, cassette ID)을 짧게 다시 요청한다
	""";

	private SchemaMetadata schemaMetadata;

	@PostConstruct
	void init() {
		this.schemaMetadata = loadSchemaMetadata();
		log.info("Loaded schema metadata: {} tables, {} relationships",
			this.schemaMetadata.tables().size(),
			this.schemaMetadata.relationships().size());
	}

	public SchemaMetadata getSchemaMetadata() {
		return this.schemaMetadata;
	}

	public String buildSchemaDescription() {
		StringBuilder builder = new StringBuilder();
		builder.append("이 시스템의 데이터베이스에는 다음 테이블이 있다.\n\n");

		for (SchemaTable table : this.schemaMetadata.tables()) {
			builder.append("- ").append(table.name()).append('\n');
			builder.append("  의미: ").append(table.purpose()).append('\n');
			builder.append("  컬럼:\n");

			for (SchemaColumn column : table.columns()) {
				builder.append("    - ")
					.append(column.name())
					.append(": ")
					.append(column.meaning())
					.append('\n');
			}

			builder.append('\n');
		}

		builder.append("테이블 관계\n");
		for (SchemaRelationship relationship : this.schemaMetadata.relationships()) {
			builder.append("- ")
				.append(relationship.from())
				.append(" -> ")
				.append(relationship.to())
				.append('\n');
		}

		builder.append('\n').append(INTERPRETATION_RULES).append('\n').append(RESPONSE_GUIDELINES);
		return builder.toString();
	}

	public String buildCapabilitySummary() {
		return """
		이 챗봇은 작업 기록 조회, 작업자 기준 조회, 공정 기준 조회, cassette 기준 조회, 날짜/기간 기준 조회, 최근 기록 조회, 건수 조회를 지원한다.
		이 챗봇은 DB를 직접 수정하지 않으며, 임의 SQL 실행도 지원하지 않는다.
		""";
	}

	@SuppressWarnings("unchecked")
	private SchemaMetadata loadSchemaMetadata() {
		try (InputStream inputStream = new ClassPathResource("schema.yml").getInputStream()) {
			Map<String, Object> root = new Yaml().load(inputStream);

			List<Map<String, Object>> tableMaps = (List<Map<String, Object>>) root.getOrDefault("tables", List.of());
			List<Map<String, Object>> relationshipMaps =
				(List<Map<String, Object>>) root.getOrDefault("relationships", List.of());

			List<SchemaTable> tables = new ArrayList<>();
			for (Map<String, Object> tableMap : tableMaps) {
				List<Map<String, Object>> columnMaps =
					(List<Map<String, Object>>) tableMap.getOrDefault("columns", List.of());
				List<SchemaColumn> columns = new ArrayList<>();
				for (Map<String, Object> columnMap : columnMaps) {
					columns.add(new SchemaColumn(
						String.valueOf(columnMap.get("name")),
						String.valueOf(columnMap.get("meaning"))
					));
				}

				tables.add(new SchemaTable(
					String.valueOf(tableMap.get("name")),
					String.valueOf(tableMap.get("purpose")),
					List.copyOf(columns)
				));
			}

			List<SchemaRelationship> relationships = new ArrayList<>();
			for (Map<String, Object> relationshipMap : relationshipMaps) {
				relationships.add(new SchemaRelationship(
					String.valueOf(relationshipMap.get("from")),
					String.valueOf(relationshipMap.get("to"))
				));
			}

			return new SchemaMetadata(List.copyOf(tables), List.copyOf(relationships));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to load schema metadata from schema.yml", ex);
		}
	}
}
