package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories.TraversalHistory;

@Directed
class Page extends Model.All {
	class Header extends Model.All {
		String name = TraversalProcessView.Ui.get().getMainCaption();

		Header() {
			bindings().from(Page.this).on(Property.history)
					.typed(TraversalHistory.class).map(this::computeName)
					.accept(this::setName);
		}

		public void setName(String name) {
			set("name", this.name, name, () -> this.name = name);
		}

		String computeName(TraversalHistory history) {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			format.append(TraversalProcessView.Ui.get().getMainCaption());
			format.appendIfNonNull(history, TraversalHistory::displayName);
			return format.toString();
		}
	}

	enum Property implements PropertyEnum {
		history
	}

	Header header;

	SelectionLayers layers;

	Properties properties;

	@Directed(className = "input")
	RenderedSelections input;

	@Directed(className = "output")
	RenderedSelections output;

	@Directed.Exclude
	TraversalHistory history;

	Page() {
		header = new Header();
		// FIXME - dirndl - bindings - change addListener to a ModelBinding with
		// a prebind (setleft) phase
		TraversalHistories.get().subscribe(null, this::setHistory);
		// bindings().addListener(() -> TraversalHistories.get().subscribe(null,
		// this::setHistory));
		bindings().from(this).on(Property.history).value(this)
				.map(SelectionLayers::new).accept(this::setLayers);
		bindings().from(this).on(Property.history).value(this)
				.map(Properties::new).accept(this::setProperties);
		bindings().from(this).on(Property.history)
				.map(o -> new RenderedSelections(this, true))
				.accept(this::setInput);
		bindings().from(this).on(Property.history)
				.map(o -> new RenderedSelections(this, false))
				.accept(this::setOutput);
	}

	public void setInput(RenderedSelections input) {
		set("input", this.input, input, () -> this.input = input);
	}

	public void setOutput(RenderedSelections output) {
		set("output", this.output, output, () -> this.output = output);
	}

	public void setLayers(SelectionLayers layers) {
		set("layers", this.layers, layers, () -> this.layers = layers);
	}

	public void setProperties(Properties properties) {
		set("properties", this.properties, properties,
				() -> this.properties = properties);
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		super.onBeforeRender(event);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
	}

	void setHistory(TraversalHistory history) {
		set(Property.history, this.history, history,
				() -> this.history = history);
	}
}
