package cc.alcina.framework.gwt.client.dirndl.overlay;

import com.google.gwt.dom.client.DomRect;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Toggle;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagTextModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

public class Expandable extends Model.Fields
		implements ModelEvents.Toggle.Handler {
	@Directed(reemits = { DomEvents.Click.class, ModelEvents.Toggle.class })
	public String contracted;

	String string;

	Overlay overlay;

	public Expandable(String string) {
		this(string, 80);
	}

	public Expandable(String string, int trimTo) {
		this.string = string;
		contracted = Ax.trim(Ax.firstLine(string), trimTo);
	}

	@Override
	public void onToggle(Toggle event) {
		if (overlay != null) {
			overlay.close(event.getContext().getOriginatingGwtEvent(), false);
		} else {
			DomRect rect = provideNode().getRendered().asElement()
					.getBoundingClientRect();
			Model contents = new TagTextModel("log", string);
			Overlay.builder().dropdown(Position.CENTER, rect, this,
					new Expanded(contents)).build().open();
		}
	}

	static class Expanded extends Model.All {
		Model contents;

		Expanded(Model contents) {
			this.contents = contents;
		}
	}
}