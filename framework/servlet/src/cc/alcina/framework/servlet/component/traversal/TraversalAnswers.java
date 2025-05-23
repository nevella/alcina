package cc.alcina.framework.servlet.component.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	boolean hasClearableFilter;

	public TraversalAnswers(int forLayer, boolean hasClearableFilter) {
		super(forLayer);
		this.hasClearableFilter = hasClearableFilter;
	}

	@Override
	public boolean checkEmptyAsk() {
		return hasClearableFilter;
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
		addExecSuggestion(query, suggestions);
		proposeSetSuggestions(query, suggestions);
		{
			// add filter selection
			AppSuggestionEntry suggestion = new AppSuggestionEntry();
			suggestion.match = Ax.format("Filter: '%s'", query);
			suggestion.modelEvent = TraversalEvents.FilterSelections.class;
			suggestion.eventData = query;
			suggestions.add(suggestion);
		}
		{
			// add command selections
			List<CommandNode> commandNodes = AppSuggestorCommands.get()
					.getCommandNodes(request, MatchStyle.any_substring);
			commandNodes.stream().map(TraversalAnswers::createSuggestion)
					.forEach(suggestions::add);
		}
		processResults(invocation, suggestions);
	}
}