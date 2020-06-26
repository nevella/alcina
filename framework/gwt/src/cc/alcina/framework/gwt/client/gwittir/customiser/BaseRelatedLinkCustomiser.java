package cc.alcina.framework.gwt.client.gwittir.customiser;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTypedHtml;

@ClientInstantiable
public abstract class BaseRelatedLinkCustomiser<T, R>
		implements Customiser, BoundWidgetProvider {
	@Override
	public BoundWidget get() {
		BaseRelatedLink link = new BaseRelatedLink();
		link.customiser = this;
		return link;
	}

	public abstract Set<R> getCollection(T model);

	public abstract String getEmptyMessage();

	public abstract BiFunction<T, R, String> getMapper();

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		return this;
	}

	public abstract String getToken(T model);

	public static class BaseRelatedLink<T, R> extends BoundTypedHtml<Set<R>> {
		T model;

		BaseRelatedLinkCustomiser<T, R> customiser;

		public BaseRelatedLink() {
		}

		@Override
		public void setModel(Object model) {
			this.model = (T) model;
			super.setModel(model);
		}

		@Override
		protected String toHtml() {
			Set<R> relateds = customiser.getCollection(model);
			if (relateds.isEmpty()) {
				return customiser.getEmptyMessage();
			}
			String template = "<a href='#%s'>View</a>\n%s";
			String token = customiser.getToken(model);
			int max = customiser.getMaxRelatedObjects();
			String relationshipsString = relateds.stream()
					.map(r -> customiser.getMapper().apply(model, r)).limit(max)
					.collect(Collectors.joining("\n"));
			if (relateds.size() > max) {
				relationshipsString += Ax.format("\n(%s)",
						CommonUtils.pluralise("item", relateds.size(), true));
			}
			return Ax.format(template, token, relationshipsString).replace("\n",
					"<br>\n");
		}
	}

	public int getMaxRelatedObjects() {
		return 10;
	}
}
