package cc.alcina.framework.gwt.client.dirndl.action;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click.Handler;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagClassModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * <p>
 * Presents an action as text (TODO - actually icon - text)
 *
 * <p>
 * FIXME - dirndl 1x1d - this is really a transform and should be modelled as
 * such (add a transform annotation to choices?)
 */
@Directed(
	tag = "action",
	bindings = @Binding(from = "title", type = Type.PROPERTY),
	receives = DomEvents.Click.class)
public class ActionIcon<U extends AbstractUiAction> extends Model
		implements DomEvents.Click.Handler {
	public final U action;

	private final TagClassModel icon;

	private final String name;

	private Handler clickHandler;

	public ActionIcon(U action, Handler clickHandler) {
		this.action = action;
		this.clickHandler = clickHandler;
		this.name = action.name();
		String className = Ax.format("icon %s", Ax.cssify(name));
		this.icon = new TagClassModel("icon", className);
	}

	@Directed
	public TagClassModel getIcon() {
		return icon;
	}

	@Directed
	public String getName() {
		return name;
	}

	public String getTitle() {
		return action.getDescription();
	}

	@Override
	public void onClick(Click event) {
		if (clickHandler != null) {
			clickHandler.onClick(event);
			WidgetUtils.squelchCurrentEvent();
		}
	}
}