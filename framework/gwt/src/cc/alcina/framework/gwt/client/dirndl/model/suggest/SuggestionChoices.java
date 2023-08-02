package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation.Type;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay.Builder;
import cc.alcina.framework.gwt.client.dirndl.overlay.Spinner;

/*
 * Implements display of suggestions via a choices model
 */
public class SuggestionChoices implements Suggestor.Suggestions,
		KeyboardNavigation.Navigation.Handler {
	private Overlay overlay;

	private boolean visible;

	private final Results contents = new Results();

	private Choices.Single<?> choices;

	private Suggestor suggestor;

	public SuggestionChoices(Suggestor suggestor) {
		this.suggestor = suggestor;
	}

	@Override
	public void close() {
		ensureVisible(false);
	}

	@Override
	public void onAnswers(Answers answers) {
		if (!visible) {
			return;
		}
		choices = new Choices.Single.Delegating<>(answers.getSuggestions());
		contents.setModel(choices);
		if (choices.getValues().size() > 0
				&& suggestor.builder.isInputEditorKeyboardNavigationEnabled()) {
			/*
			 * keyboard-navigate to the first entry
			 */
			Navigation event = new Navigation();
			event.setTypedModel(Type.FIRST);
			onNavigation(event);
		}
	}

	@Override
	public void onAskException(Throwable throwsable) {
		if (!visible) {
			return;
		}
		// FIXME - design
		contents.setModel(
				new String(CommonUtils.toSimpleExceptionMessage(throwsable)));
	}

	/*
	 * routed from parent suggestor (since this is a peer, not a node.model)
	 */
	@Override
	public void onClosed(Closed event) {
		ensureVisible(false);
	}

	@Override
	public void onNavigation(Navigation event) {
		switch (event.getModel()) {
		case CANCEL:
			event.consume();
			ensureVisible(false);
			break;
		default:
			if (choices == null) {
				if (!GWT.isScript()) {
					Ax.err("warn - commit before choices");
					return;
				}
			}
			choices.onNavigation(event);
			break;
		}
	}

	@Override
	public Object provideSelectedValue() {
		return choices.provideSelectedValue();
	}

	@Override
	public void toState(State state) {
		switch (state) {
		case UNBOUND:
			ensureVisible(false);
			break;
		case LOADING:
			ensureVisible(true);
			break;
		default:
			// do not ensure overlay - if it's dismissed, something (user
			// gesture) happened since LOADING
			break;
		}
		switch (state) {
		case LOADING:
			contents.setModel(Spinner.builder().generate());
			break;
		}
	}

	void ensureVisible(boolean ensure) {
		if (visible == ensure) {
			return;
		}
		if (!ensure) {
			int debug = 3;
		}
		NodeEvent.Context.fromNode(suggestor.provideNode())
				.dispatch(SuggestorEvents.SuggestionsVisible.class, ensure);
		if (ensure) {
			visible = true;
			if (useOverlay()) {
				Builder builder = Overlay.builder();
				builder.dropdown(suggestor.builder.getSuggestionXAlign(),
						suggestor.provideElement().getBoundingClientRect(),
						suggestor, contents).withLogicalAncestors(
								suggestor.builder.getLogicalAncestors());
				overlay = builder.build();
				overlay.open();
			} else {
				suggestor.setNonOverlaySuggestionResults(contents);
			}
		} else {
			Scheduler.get().scheduleDeferred(() -> {
				if (useOverlay()) {
					overlay.close(null, false);
					overlay = null;
				} else {
					suggestor.setNonOverlaySuggestionResults(null);
				}
			});
			visible = false;
		}
	}

	boolean useOverlay() {
		return !suggestor.builder.isNonOverlaySuggestionResults();
	}

	@Directed
	public static class Results extends Model {
		private Object model;

		@Directed
		public Object getModel() {
			return this.model;
		}

		public void setModel(Object model) {
			var old_model = this.model;
			this.model = model;
			propertyChangeSupport().firePropertyChange("model", old_model,
					model);
		}
	}
}
