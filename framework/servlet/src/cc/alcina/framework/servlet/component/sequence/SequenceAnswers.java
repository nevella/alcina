package cc.alcina.framework.servlet.component.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestion;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl.Invocation;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.CommandNode;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommands.MatchStyle;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorRequest;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.servlet.component.traversal.TraversalEvents;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;

class SequenceAnswers implements AppSuggestor.AnswerSupplier {
	public SequenceAnswers() {
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
		request.commandContexts.add(SequenceBrowser.CommandContext.class);
		List<AppSuggestion> suggestions = new ArrayList<>();
		addLoadSuggestion(query, suggestions);
		addHighlightSuggestion(query, suggestions);
		addSetSuggestion(query, suggestions);
		addExecSuggestion(query, suggestions);
		{
			// add filter selection
			AppSuggestionEntry suggestion = new AppSuggestionEntry();
			suggestion.match = Ax.format("Filter: '%s'", query);
			suggestion.modelEvent = SequenceEvents.FilterElements.class;
			suggestion.eventData = query;
			suggestions.add(suggestion);
		}
		{
			// add command selections
			List<CommandNode> commandNodes = AppSuggestorCommands.get()
					.getCommandNodes(request, MatchStyle.any_substring);
			commandNodes.stream().map(SequenceAnswers::createSuggestion)
					.forEach(suggestions::add);
		}
		processResults(invocation, suggestions);
	}

	void addLoadSuggestion(String query, List<AppSuggestion> suggestions) {
		Pattern pattern = Pattern.compile("load (\\S+)");
		Matcher matcher = pattern.matcher(query);
		if (matcher.matches()) {
			AppSuggestionEntry suggestion = new AppSuggestionEntry();
			suggestion.eventData = matcher.group(1);
			suggestion.match = Ax.format("Load sequence: '%s'",
					suggestion.eventData);
			suggestion.modelEvent = SequenceEvents.LoadSequence.class;
			suggestions.add(suggestion);
		}
	}

	void addHighlightSuggestion(String query, List<AppSuggestion> suggestions) {
		Pattern pattern = Pattern.compile("hi (\\S.*)");
		Matcher matcher = pattern.matcher(query);
		if (matcher.matches()) {
			AppSuggestionEntry suggestion = new AppSuggestionEntry();
			suggestion.eventData = matcher.group(1);
			suggestion.match = Ax.format("Highlight: '%s'",
					suggestion.eventData);
			suggestion.modelEvent = SequenceEvents.HighlightElements.class;
			suggestions.add(suggestion);
		}
	}

	void addSetSuggestion(String query, List<AppSuggestion> suggestions) {
		{
			Pattern pattern = Pattern.compile("set rows (\\d+)");
			Matcher matcher = pattern.matcher(query);
			if (matcher.matches()) {
				AppSuggestionEntry suggestion = new AppSuggestionEntry();
				suggestion.eventData = matcher.group(1);
				int tableRows = SequenceSettings.get().maxElementRows;
				suggestion.match = Ax.format("Set rows: '%s' (current=%s)",
						suggestion.eventData, tableRows);
				suggestion.modelEvent = SequenceEvents.SetSettingMaxElementRows.class;
				suggestions.add(suggestion);
			}
		}
	}

	void addExecSuggestion(String query, List<AppSuggestion> suggestions) {
		{
			Pattern pattern = Pattern.compile("exec (\\S+)");
			Matcher matcher = pattern.matcher(query);
			if (matcher.matches()) {
				AppSuggestionEntry suggestion = new AppSuggestionEntry();
				suggestion.eventData = matcher.group(1);
				suggestion.match = Ax.format(
						"Exec '%s' ['l' lists available commands]",
						suggestion.eventData);
				suggestion.modelEvent = SequenceEvents.ExecCommand.class;
				suggestions.add(suggestion);
			}
		}
	}
}