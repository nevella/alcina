package cc.alcina.framework.gwt.client.dirndl.model.edit;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Close;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ContentDecoratorEvents.ReferenceSelected;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answer;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.TagEditor;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

/**
 * <p>
 * This component allows the user to select the object (normally an entity) that
 * will be referenced by the decoration element in the contenteditable area - an
 * example would be selection of a domain user 'MyName' which is then referenced
 * by a '@MyName' mention in the text
 *
 * <p>
 * Subclasses just need to customise the trigger sequence ('@' in the above
 * example) and the Suggestor {@link Answer} implementation which provides
 * entities filter on user text
 */
@Directed(
	receives = { ModelEvents.SelectionChanged.class, ModelEvents.Close.class },
	emits = ModelEvents.Selected.class)
public abstract class DecoratorChooser extends Model.Fields implements
		ModelEvents.SelectionChanged.Handler, ModelEvents.Close.Handler {
	protected final ContentDecorator<?> contentDecorator;

	@Directed
	public Suggestor suggestor;

	protected DomNode decoratorNode;

	protected String triggerSequence;

	protected TagEditor tagEditor;

	public DecoratorChooser(ContentDecorator contentDecorator,
			DomNode decoratorNode, String triggerSequence) {
		this.contentDecorator = contentDecorator;
		this.decoratorNode = decoratorNode;
		this.triggerSequence = triggerSequence;
		init();
	}

	@Override
	public void onClose(Close event) {
		this.contentDecorator.cleanupInvalidDecorators();
		this.contentDecorator.chooser = null;
		if (event != null) {
			event.getContext().bubble();
		}
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		event.reemitAs(this, ReferenceSelected.class,
				suggestor.provideSelectedValue());
		/*
		 * When an element is selected (in the suggestor), close
		 */
		event.reemitAs(this, ModelEvents.Close.class);
	}

	protected Suggestor.Builder createSuggestorBuilder() {
		Suggestor.Builder builder = Suggestor.builder();
		builder.withFocusOnBind(true);
		builder.withSuggestionXAlign(Position.END);
		builder.withSuggestOnBind(true);
		builder.withEditorSupplier(() -> tagEditor);
		return builder;
	}

	protected TagEditor createTagEditor() {
		return new TagEditor((Element) this.decoratorNode.w3cElement(),
				ask -> ask.replaceFirst("^" + triggerSequence, ""));
	}

	protected void init() {
		tagEditor = createTagEditor();
		suggestor = createSuggestorBuilder().build();
		bindings().addListener(
				() -> this.contentDecorator.topicInput.add(tagEditor::onInput));
	}

	public interface Provider {
		DecoratorChooser provideChooser(ContentDecorator contentDecorator,
				DomNode decorator, String triggerSequence);
	}
}
