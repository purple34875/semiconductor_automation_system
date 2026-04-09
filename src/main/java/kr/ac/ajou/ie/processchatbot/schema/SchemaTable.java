package kr.ac.ajou.ie.processchatbot.schema;

import java.util.List;

public record SchemaTable(
	String name,
	String purpose,
	List<SchemaColumn> columns
) {
}
