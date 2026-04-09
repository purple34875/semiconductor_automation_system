package kr.ac.ajou.ie.processchatbot.queryplan.sql;

import java.util.Map;

public record SqlQuerySpec(
	String sql,
	Map<String, Object> params,
	SqlResultMode resultMode
) {
}
