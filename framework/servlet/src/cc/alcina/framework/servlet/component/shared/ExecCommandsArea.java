package cc.alcina.framework.servlet.component.shared;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.Filterable;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.KeyboardShortcutsArea;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.FilterableAskAnswer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOnBind;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion.ModelSuggestion;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * <p>
 * Models a view of all exec commands
 * 
 * @see KeyboardShortcutsArea
 */
@DirectedContextResolver
public class ExecCommandsArea extends Model.All
		implements ModelEvents.SelectionChanged.Handler {
	Heading heading = new Heading("Exec commands");

	Suggestor suggestor;

	@Property.Not
	List<CommandArea> commandAreas;

	public ExecCommandsArea(Stream<? extends ExecCommand> commands) {
		commandAreas = commands.map(CommandArea::new).sorted()
				.collect(Collectors.toList());
		Suggestor.Attributes attributes = Suggestor.attributes();
		attributes.withFocusOnBind(true);
		attributes.withAnswer(new AnswerImpl());
		attributes.withNonOverlaySuggestionResults(true);
		attributes.withInputPrompt("Filter metadata");
		attributes.withSuggestOnBind(SuggestOnBind.YES);
		attributes.withInputEditorKeyboardNavigationEnabled(true);
		suggestor = attributes.create();
		// commandChoices=new SuggestionChoices(suggestor);
		// input.setPlaceholder("Filter metadata");
	}

	class AnswerImpl implements Answer<StringAsk> {
		@Override
		public void ask(StringAsk ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler) {
			FilterableAskAnswer<CommandArea> router = new FilterableAskAnswer<>();
			Answers answers = router.ask(ask, commandAreas);
			answersHandler.accept(answers);
		}
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		ModelSuggestion suggestion = event.getModel();
		event.reemitAs(this, ExecCommand.PerformCommand.class,
				((CommandArea) suggestion.getModel()).name);
		event.reemitAs(this, ModelEvents.Close.class);
	}

	@Directed(tag = "command-area")
	class CommandArea extends Filterable.FilterFilterable.Abstract
			implements Comparable<CommandArea>, DomEvents.Click.Handler {
		String name;

		String description;

		CommandArea(ExecCommand command) {
			name = command.name();
			description = command.description();
		}

		@Override
		public boolean matchesFilter(String filterString) {
			return SearchUtils.containsIgnoreCase(filterString, name,
					description);
		}

		@Override
		public int compareTo(CommandArea o) {
			return name.compareTo(o.name);
		}

		@Override
		public void onClick(Click event) {
			event.reemitAs(this, ExecCommand.PerformCommand.class, name);
		}
	}
}
