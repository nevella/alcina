package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Choice;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.StringAskAnswer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement;
import cc.alcina.framework.gwt.client.logic.CancellableAsyncCallback;

public class ChoiceSuggestions extends DecoratorSuggestions {
	MultipleSuggestions multipleSuggestions;

	ChoiceSuggestions(MultipleSuggestions multipleSuggestions,
			ContentDecorator contentDecorator, DomNode decoratorNode) {
		super(contentDecorator, decoratorNode);
		this.multipleSuggestions = multipleSuggestions;
	}

	@Override
	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = super.createSuggestorAttributes();
		attributes.withInputPrompt("Select...");
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
		public void ask(StringAsk ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler) {
			// FIXME - FN - there's probably more layers here than needed. First
			// step (funny that): docco the process
			/*
			 * Enums or suchlike
			 */
			List<?> values = multipleSuggestions.getValues();
			SuggestOracle.Response response = StringAskAnswer
					.selectValues(values, ask);
			handleSuggestionResponse(ask, answersHandler, response);
		}

		protected void handleSuggestionResponse(StringAsk ask,
				Consumer<Answers> answersHandler,
				SuggestOracle.Response response) {
			Collection<? extends Suggestion> suggestions = response
					.getSuggestions();
			List<Choice> choices = suggestions.stream().map(s -> new Choice(
					((BoundSuggestOracleResponseElement.UntypedSuggestion) s).suggestion))
					.collect(Collectors.toList());
			StringAskAnswer<Choice> router = new StringAskAnswer<>();
			Answers answers = router.ask(ask, choices, Object::toString);
			answersHandler.accept(answers);
		}
	}
}