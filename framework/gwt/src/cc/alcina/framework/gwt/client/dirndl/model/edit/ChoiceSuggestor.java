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

/*
 * Manages the dropdown suggestions for the ChoiceSuggestions editor
 * 
 * Doc :: behavior :: repeatablechoices - if repeatablechoices is not set (on
 * the Choices), filter the ask answers by existing choices
 */
public class ChoiceSuggestor extends DecoratorSuggestor {
	ChoiceEditor choiceEditor;

	ChoiceSuggestor(ChoiceEditor choiceSuggestions,
			ContentDecorator contentDecorator, DomNode decoratorNode) {
		super(contentDecorator, decoratorNode);
		this.choiceEditor = choiceSuggestions;
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
			List<?> values = choiceEditor.getValues();
			SuggestOracle.Response response = StringAskAnswer
					.selectValues(values, ask);
			handleSuggestionResponse(ask, answersHandler, response);
		}

		protected void handleSuggestionResponse(StringAsk ask,
				Consumer<Answers> answersHandler,
				SuggestOracle.Response response) {
			Collection<? extends Suggestion> suggestions = response
					.getSuggestions();
			List<?> suggestedObjects = suggestions.stream().map(
					s -> ((BoundSuggestOracleResponseElement.UntypedSuggestion) s).suggestion)
					.collect(Collectors.toList());
			/*
			 * see DecoratorBehavior.RepeatableChoiceHandling
			 */
			if (!choiceEditor.isRepeatableChoices()) {
				List<?> selectedValues = choiceEditor.getEditorValues();
				suggestedObjects.removeIf(selectedValues::contains);
			}
			List<Choice> choices = suggestedObjects.stream().map(Choice::new)
					.collect(Collectors.toList());
			StringAskAnswer<Choice> router = new StringAskAnswer<>();
			Answers answers = router.ask(ask, choices, Object::toString);
			answersHandler.accept(answers);
		}
	}
}