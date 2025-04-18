/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.gwittir.customiser;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.customiser.RenderedLabelCustomiser.RenderedLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.ReflectInstantiableToStringRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracle;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement;

@Reflected
/**
 *
 * @author Nick Reddel
 * 
 *         Note - this *can* be used with non HasLocalId objects -
 *         "domainobject" is more an indication that "comes from server"
 */
public class DomainObjectSuggestCustomiser
		implements Customiser, BoundWidgetProvider {
	public static final String TARGET_CLASS = "targetClass";

	public static final String RENDERER_CLASS = "rendererClass";

	public static final String READONLY_CUSTOMISER_CLASS = "readonlyCustomiserClass";

	public static final String HINT = "hint";

	public static final String SHOW_ON_FOCUS = "showOnFocus";

	public static final String WITH_PLACEHOLDER = "with-placeholder";

	public static final String PLACEHOLDER = "WITH_PLACEHOLDER";

	private Class classValue;

	private Class rendererClassValue = ReflectInstantiableToStringRenderer.class;

	private String hintValue = "";

	private Class readonlyCustomiserClassValue;

	private boolean showOnFocus;

	private boolean withPlaceholder;

	private String placeholderText;

	private String cssClassName = "";

	private String suggestBoxCssClassName = "";

	@Override
	public BoundWidget get() {
		BoundSuggestBox boundSuggestBox = new BoundSuggestBox<>();
		boundSuggestBox.setRenderer(getRenderer());
		boundSuggestBox.setWithPlaceholder(withPlaceholder);
		boundSuggestBox.setPlaceholderText(placeholderText);
		boundSuggestBox.suggestOracle(
				new BoundSuggestOracle().clazz(classValue).hint(hintValue));
		boundSuggestBox.setShowOnFocus(showOnFocus);
		boundSuggestBox.setStyleName(cssClassName);
		boundSuggestBox.setSuggestBoxStyleName(suggestBoxCssClassName);
		return boundSuggestBox;
	}

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom custom) {
		return getProvider(editable, objectClass, multiple, custom, null);
	}

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom custom,
			AnnotationLocation propertyLocation) {
		classValue = NamedParameter.Support.classValue(custom.parameters(),
				TARGET_CLASS, void.class);
		rendererClassValue = NamedParameter.Support.classValue(
				custom.parameters(), RENDERER_CLASS,
				BoundSuggestOracleResponseTypeRenderer.class);
		readonlyCustomiserClassValue = NamedParameter.Support.classValue(
				custom.parameters(), READONLY_CUSTOMISER_CLASS, null);
		hintValue = NamedParameter.Support.stringValue(custom.parameters(),
				HINT, "");
		showOnFocus = NamedParameter.Support.booleanValue(custom.parameters(),
				SHOW_ON_FOCUS);
		withPlaceholder = NamedParameter.Support
				.booleanValueDefaultTrue(custom.parameters(), WITH_PLACEHOLDER);
		placeholderText = NamedParameter.Support.stringValue(
				custom.parameters(), PLACEHOLDER, "Type for suggestions");
		Args args = propertyLocation.getAnnotation(Args.class);
		if (args != null) {
			classValue = args.targetClass();
			showOnFocus = args.showOnFocus();
			cssClassName = args.cssClassName();
			hintValue = args.hint();
			placeholderText = args.placeholder();
			rendererClassValue = args.rendererClass();
		}
		return editable ? this
				: readonlyCustomiserClassValue == null
						? new RenderedLabelProvider(rendererClassValue, null)
						: ((Customiser) Reflections
								.newInstance(readonlyCustomiserClassValue))
										.getProvider(editable, objectClass,
												multiple, custom);
	}

	public Renderer getRenderer() {
		return (Renderer) Reflections.newInstance(rendererClassValue);
	}

	public boolean isShowOnFocus() {
		return this.showOnFocus;
	}

	public DomainObjectSuggestCustomiser withClassValue(Class classValue) {
		this.classValue = classValue;
		return this;
	}

	public DomainObjectSuggestCustomiser withCssClassName(String cssClassName) {
		this.cssClassName = cssClassName;
		return this;
	}

	public DomainObjectSuggestCustomiser withHint(String hint) {
		this.hintValue = hint;
		return this;
	}

	public DomainObjectSuggestCustomiser
			withRendererClassName(Class rendererClassValue) {
		this.rendererClassValue = rendererClassValue;
		return this;
	}

	public DomainObjectSuggestCustomiser withShowOnFocus(boolean showOnFocus) {
		this.showOnFocus = showOnFocus;
		return this;
	}

	public DomainObjectSuggestCustomiser
			withSuggestBoxCssClassName(String suggestBoxCssClassName) {
		this.suggestBoxCssClassName = suggestBoxCssClassName;
		return this;
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Args {
		String cssClassName() default "";

		String hint() default "";

		String placeholder() default "Type for suggestions";

		Class<? extends Renderer> rendererClass() default BoundSuggestOracleResponseTypeRenderer.class;

		boolean showOnFocus() default false;

		Class<? extends BoundSuggestOracleResponseElement> targetClass();
	}

	@Reflected
	public static class BoundSuggestOracleResponseTypeRenderer
			implements Renderer<BoundSuggestOracleResponseElement, String> {
		@Override
		public String render(BoundSuggestOracleResponseElement o) {
			return o == null ? null : o.toSuggestionResultString();
		}
	}

	@Reflected
	public static class BoundSuggestOracleResponseTypeSuggestionRenderer
			implements Renderer<BoundSuggestOracleResponseElement, String> {
		@Override
		public String render(BoundSuggestOracleResponseElement o) {
			return o == null ? null : o.toSuggestionString();
		}
	}
}