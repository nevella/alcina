package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.domain.HasObject;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Choice;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.StringAskAnswer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.logic.CancellableAsyncCallback;

/*
 * Manages the dropdown suggestions for the ChoiceSuggestions editor
 * 
 * Doc :: behavior :: repeatablechoices - if repeatablechoices is not set (on
 * the Choices), filter the ask answers by existing choices
 */
public class ChoiceSuggestor extends DecoratorSuggestor {
	ChoiceEditor<?> choiceEditor;

	Function<Object, Model> valueTransformer;

	ChoiceSuggestor(ChoiceEditor choiceEditor,
			ContentDecorator contentDecorator, DomNode decoratorNode,
			Function<Object, Model> valueTransformer) {
		super(contentDecorator, decoratorNode);
		this.choiceEditor = choiceEditor;
		this.valueTransformer = valueTransformer;
		init0();
	}

	@Override
	protected void init() {
		// noop, to allow init0 post-super-constructor
	}

	@Override
	protected Suggestor.Attributes createSuggestorAttributes() {
		Suggestor.Attributes attributes = super.createSuggestorAttributes();
		attributes.withInputPrompt("Select...");
		attributes.withAnswer(new AnswerImpl());
		return attributes;
	}

	/*
	 * Gets a list of Answer objects (wrapping returned objects) that match the
	 * decorator text
	 */
	class AnswerImpl implements Answer<StringAsk> {
		protected CancellableAsyncCallback runningCallback = null;

		@Override
		public void ask(StringAsk ask, Consumer<Answers> answersHandler,
				Consumer<Throwable> exceptionHandler) {
			if (choiceEditor.suggestOracleRouter != null) {
				choiceEditor.suggestOracleRouter.ask(provideNode(), ask,
						response -> handleSuggestionResponse(ask,
								answersHandler,
								(SuggestOracle.Response) response));
			} else {
				/*
				 * Enums or suchlike
				 */
				List<?> values = choiceEditor.getValues();
				SuggestOracle.Response response = StringAskAnswer
						.selectValues(values, ask);
				handleSuggestionResponse(ask, answersHandler, response);
			}
		}

		protected void handleSuggestionResponse(StringAsk ask,
				Consumer<Answers> answersHandler,
				SuggestOracle.Response response) {
			if (provideIsUnbound()) {
				return;
			}
			Collection<? extends Suggestion> suggestions = response
					.getSuggestions();
			List<?> suggestedObjects = suggestions.stream()
					.map(s -> ((HasObject) s).provideObject())
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
			boolean keyboardSelectFirst = false;
			if (choiceEditor.suggestOracleRouter != null) {
				// pre-filtered
				ask = new StringAsk();
				ask.setValue("");
				keyboardSelectFirst = choices.size() > 0;
			}
			Answers answers = router.ask(ask, choices, Object::toString,
					valueTransformer);
			answers.keyboardSelectFirst = keyboardSelectFirst;
			answersHandler.accept(answers);
		}
	}
}