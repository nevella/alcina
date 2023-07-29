package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Date;
import java.util.function.Function;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 *
 * Transforms an input model to a renderable model. Note that the renderable
 * model class must be annotated with @Directed (or have a default renderer -
 * which classes such as String, Collection etc do)
 *
 *
 *
 * 
 *
 */
@Reflected
public interface ModelTransform<A, B> extends Function<A, B> {
	public abstract static class AbstractContextSensitiveModelTransform<A, B>
			extends AbstractModelTransform<A, B>
			implements ContextSensitiveTransform<A, B> {
		protected Node node;

		@Override
		public AbstractContextSensitiveModelTransform<A, B>
				withContextNode(Node node) {
			this.node = node;
			return this;
		}
	}

	@Reflected
	public abstract static class AbstractModelTransform<A, B>
			implements ModelTransform<A, B> {
	}

	/**
	 * Note that the context DirectedLayout.Node only exposes its own properties
	 * (not parent/children) to classes outside the main dirndl package.
	 *
	 * 
	 *
	 * @param <A>
	 *            The incoming model
	 * @param <B>
	 *            The outgoing model
	 */
	public interface ContextSensitiveTransform<A, B>
			extends ModelTransform<A, B> {
		public ContextSensitiveTransform<A, B>
				withContextNode(DirectedLayout.Node node);
	}

	public static class DateStyle_TimestampHuman
			implements ModelTransform<Date, String> {
		@Override
		public String apply(Date t) {
			return CommonUtils.formatDate(t, DateStyle.TIMESTAMP_HUMAN);
		}
	}

	public static class Placeholder
			extends AbstractModelTransform<Object, Bindable> {
		@Override
		public Bindable apply(Object t) {
			PlaceholderModel model = new PlaceholderModel();
			return model;
		}
	}

	public static class PlaceholderModel extends Model {
		private String value = "Placeholder";

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
