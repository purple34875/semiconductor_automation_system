package kr.ac.ajou.ie.processchatbot.schema;

import java.util.List;

public record SchemaMetadata(
	List<SchemaTable> tables,
	List<SchemaRelationship> relationships
) {
}
