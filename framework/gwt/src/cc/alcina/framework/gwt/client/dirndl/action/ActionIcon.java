package cc.alcina.framework.gwt.client.dirndl.action;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagClassModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

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
	bindings = @Binding(from = "title", type = Type.PROPERTY))
public class ActionIcon<U extends AbstractUiAction> extends Model {
	public final U action;

	private final TagClassModel icon;

	private final String name;

	public ActionIcon(U action) {
		this.action = action;
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
}