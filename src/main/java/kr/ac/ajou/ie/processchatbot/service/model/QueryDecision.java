package kr.ac.ajou.ie.processchatbot.service.model;

public record QueryDecision(
	QueryType queryType,
	SelectedAction selectedAction,
	ExtractedParameters parameters,
	String normalizedQuestion
) {

	public boolean usesDatabase() {
		return this.selectedAction.usesDatabase();
	}
}
