package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
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

	private final Contents contents = new Contents();

	private Choices.Single<?> choices;

	private Suggestor suggestor;

	public SuggestionChoices(Suggestor suggestor) {
		this.suggestor = suggestor;
	}

	@Override
	public void onAnswers(Answers answers) {
		if (overlay == null) {
			return;
		}
		choices = new Choices.Single.Delegating<>(answers.getSuggestions());
		contents.setModel(choices);
	}

	@Override
	public void onAskException(Throwable throwsable) {
		if (overlay == null) {
			return;
		}
		// FIXME - design
		contents.setModel(
				new String(CommonUtils.toSimpleExceptionMessage(throwsable)));
	}

	@Override
	public void onNavigation(Navigation event) {
		switch (event.getModel()) {
		case CANCEL:
			event.consume();
			overlay.close(event.getContext().getOriginatingGwtEvent(), false);
			break;
		default:
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
			ensureOverlay(false);
			break;
		case LOADING:
			ensureOverlay(true);
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

	private void ensureOverlay(boolean ensure) {
		if (ensure) {
			if (overlay == null) {
				Builder builder = Overlay.builder();
				builder.dropdown(suggestor.builder.getSuggestionXAlign(),
						suggestor.provideElement().getBoundingClientRect(),
						suggestor, contents).withLogicalAncestors(
								suggestor.builder.getLogicalAncestors());
				overlay = builder.build();
				overlay.open();
			}
		} else {
			if (overlay != null) {
				overlay.close(null, false);
				overlay = null;
			}
		}
	}

	@Directed.Delegating
	public static class Contents extends Model {
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
