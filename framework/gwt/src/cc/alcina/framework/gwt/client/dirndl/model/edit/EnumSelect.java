package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Choice;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Select;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContext;

@Directed.Delegating
@Bean(PropertySource.FIELDS)
@Registration({ Model.Value.class, FormModel.Editor.class, Enum.class })
public class EnumSelect<E extends Enum> extends Model.Value<E>
		implements ModelEvents.SelectionChanged.Handler {
	private static final transient String DEFAULT_NULL_STRING = "[Null]";

	@Directed
	@DirectedContextResolver(EnumSelect.SelectResolver.class)
	public Choices.Select<E> select;

	private E value;

	@Override
	public E getValue() {
		return value;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		Node node = event.getContext().node;
		ContextResolver resolver = node.getResolver();
		NodeEditorContext context = NodeEditorContext.get(resolver);
		select = new Select<>();
		Class<E> type = context.getEditingProperty().getType();
		List<E> values = Arrays.stream(type.getEnumConstants())
				.collect(Collectors.toList());
		Null _null = node.annotation(Null.class);
		boolean withNull = _null == null || _null.with();
		if (withNull) {
			values.add(0, null);
		}
		select.setValues(values);
		bindings().from(this).on("value").to(select).on("selectedValue").bidi();
		super.onBeforeRender(event);
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		setValue(event.getModel());
	}

	@Override
	public void setValue(E value) {
		set("value", this.value, value, () -> this.value = value);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Null {
		/**
		 * The display string for a null select value
		 */
		String value() default DEFAULT_NULL_STRING;

		/**
		 * Whether the select should allow null (it will be the first option)
		 */
		boolean with() default true;
	}

	public static class SelectResolver extends Choices.SelectResolver {
		String nullString = DEFAULT_NULL_STRING;

		@Override
		protected void init(Node node) {
			super.init(node);
			Null _null = node.annotation(Null.class);
			if (_null != null) {
				nullString = _null.value();
			}
		}

		@Override
		protected String transformOptionName(Node node, Choice choice) {
			Object input = choice.getValue();
			if (input == null) {
				return nullString;
			} else {
				return super.transformOptionName(node, choice);
			}
		}
	}
}
