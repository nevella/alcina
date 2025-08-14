package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.Arrays;
import java.util.function.Consumer;

import com.google.gwt.dom.client.DomRect;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Toggle;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.util.Async;

public class Expandable extends Model.Fields
		implements ModelEvents.Toggle.Handler {
	@Directed(reemits = { DomEvents.Click.class, ModelEvents.Toggle.class })
	public String contracted;

	String string;

	Overlay overlay;

	transient Consumer<AsyncCallback<String>> fullSupplier;

	public Expandable(String string) {
		this(string, 80, "", null);
	}

	/*
	 * Note that this won't work in (say) a table yet - use an Expandable
	 * property. FIXME - dirndl - this is really best solved by the full
	 * annotation derivation history
	 */
	public static class To implements ModelTransform<String, Expandable> {
		@Override
		public Expandable apply(String t) {
			return new Expandable(t);
		}
	}

	public Expandable(String string, int trimTo, String blankMessage,
			Consumer<AsyncCallback<String>> fullSupplier) {
		this.string = Ax.blankToEmpty(string);
		this.fullSupplier = fullSupplier;
		String firstNonEmptyLine = Arrays.stream(this.string.split("\n"))
				.map(Ax::ntrim).filter(s -> s.length() > 0).findFirst()
				.orElse(blankMessage);
		contracted = Ax.trim(firstNonEmptyLine, trimTo);
	}

	@Override
	public void onToggle(Toggle event) {
		if (overlay != null) {
			overlay.close(event.getContext().getOriginatingGwtEvent(), false);
		} else {
			DomRect rect = provideNode().getRendered().asElement()
					.getBoundingClientRect();
			Log contents = new Log(string);
			Overlay.attributes().dropdown(Position.CENTER, rect, this,
					new Expanded(contents)).create().open();
			if (fullSupplier != null) {
				fullSupplier.accept(Async.<String> callbackBuilder()
						.success(contents::setString).build());
			}
		}
	}

	static class Log extends Model.All {
		@Binding(type = Type.INNER_TEXT)
		String string;

		public void setString(String string) {
			set("string", this.string, string, () -> this.string = string);
		}

		Log(String string) {
			this.string = string;
		}
	}

	static class Expanded extends Model.All {
		Model contents;

		Expanded(Model contents) {
			this.contents = contents;
		}
	}
}