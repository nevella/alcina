package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Choice;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.StringAskAnswer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleSuggestion;
import cc.alcina.framework.gwt.client.logic.CancellableAsyncCallback;

public class ChoiceChooser extends DecoratorChooser {
	ChoiceChooser(ContentDecorator contentDecorator, DomNode decoratorNode) {
		super(contentDecorator, decoratorNode);
	}

	@Override
	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = super.createSuggestorAttributes();
		attributes.withInputPrompt("Select user");
		attributes.withAnswer(new AnswerImpl());
		return attributes;
	}

	/*
	 * Gets a list of Answer objects (wrapping JadeUser objects) that match the
	 * decorator text
	 */
	class AnswerImpl implements Answer<StringAsk> {
		protected CancellableAsyncCallback runningCallback = null;

		@Override
		// FIXME - DCA1x1 - exceptionHandler
		public void ask(StringAsk ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler) {
			BoundSuggestOracleRequest boundRequest = new BoundSuggestOracleRequest();
			boundRequest.setQuery(ask.getValue());
			boundRequest.setTargetClassName(Choice.class.getName());
			boundRequest.setHint("blah");
			Optional.ofNullable(runningCallback)
					.ifPresent(sc -> sc.setCancelled(true));
			runningCallback = new CancellableAsyncCallback<SuggestOracle.Response>() {
				@Override
				public void onSuccess(SuggestOracle.Response result) {
					handleSuggestionResponse(ask, answersHandler, result);
				}
			};
			Client.searchRemoteService().suggest(boundRequest, runningCallback);
		}

		protected void handleSuggestionResponse(StringAsk ask,
				Consumer<Answers> answersHandler,
				SuggestOracle.Response result) {
			Collection<? extends Suggestion> suggestions = result
					.getSuggestions();
			List<Choice> users = suggestions.stream()
					.map(s -> (Choice) ((BoundSuggestOracleSuggestion) s)
							.getTypedValue())
					.filter(Objects::nonNull).collect(Collectors.toList());
			StringAskAnswer<Choice> router = new StringAskAnswer<>();
			Answers answers = router.ask(ask, users, Object::toString);
			answersHandler.accept(answers);
		}
	}
}