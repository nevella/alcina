package cc.alcina.framework.servlet.component.traversal;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestion;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl.Invocation;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.CommandNode;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.MatchStyle;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorRequest;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.TraversalAnswerSupplier;

class TraversalAnswers extends TraversalAnswerSupplier {
	public TraversalAnswers(int forLayer) {
		super(forLayer);
	}

	static AppSuggestion createSuggestion(CommandNode node) {
		AppSuggestionEntry suggestion = new AppSuggestionEntry();
		suggestion.modelEvent = (Class<? extends ModelEvent>) node.eventClass;
		suggestion.match = node.toPath();
		suggestion.secondary = node.command.description();
		return suggestion;
	}

	@Override
	public void begin(Invocation invocation) {
		AppSuggestorRequest request = new AppSuggestorRequest();
		String query = invocation.ask.getValue();
		request.setQuery(query);
		request.commandContexts.add(TraversalBrowser.CommandContext.class);
		List<AppSuggestion> suggestions = new ArrayList<>();
		AppSuggestionEntry suggestion = new AppSuggestionEntry();
		suggestion.match = Ax.format("Filter: '%s'", query);
		suggestion.modelEvent = TraversalEvents.FilterSelections.class;
		suggestion.eventData = query;
		// suggestion.
		suggestions.add(suggestion);
		proposeSetSuggestions(query, suggestions);
		List<CommandNode> commandNodes = AppSuggestorCommands.get()
				.getCommandNodes(request, MatchStyle.any_substring);
		commandNodes.stream().map(TraversalAnswers::createSuggestion)
				.forEach(suggestions::add);
		processResults(invocation, suggestions);
	}
}