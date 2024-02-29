package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusin;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * <p>
 * This class models an editable text field, rendering as either an
 * <code>&lt;input&gt;</code> or <code>&lt;textarea&gt;</code> DOM element.
 *
 * <p>
 * It maintains a 'currentValue' r/o property, tracking the current edited (but
 * not committed) value of the element (an element is committed - and the value
 * property changed - on focus loss or [enter] in the case of input elements -
 * not on every change to the visible text) - see
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/input_event
 *
 * <p>
 * Note that the StringInput.value property may be out of sync with the
 * element.value property - since the StringInput.value property tracks the
 * committed value
 *
 * <p>
 * It fires modelevents <code>&lt;Change&gt;</code> and
 * <code>&lt;Input&gt;</code>, wrapping the corresponding DOM events
 *
 *
 * FIXME - dirndl - to Model.Fields
 *
 */
@Directed(
	bindings = { @Binding(type = Type.PROPERTY, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder"),
			@Binding(type = Type.PROPERTY, from = "type"),
			@Binding(type = Type.PROPERTY, from = "spellcheck"),
			@Binding(type = Type.PROPERTY, from = "autocomplete"),
			@Binding(type = Type.PROPERTY, from = "rows"),
			@Binding(type = Type.PROPERTY, from = "disabled") },
	emits = { ModelEvents.Change.class, ModelEvents.Input.class,
			ModelEvents.Commit.class })
@Registration({ Model.Value.class, FormModel.Editor.class, String.class })
@Registration({ Model.Value.class, FormModel.Editor.class, Number.class })
public class StringInput extends Model.Value<String> implements FocusOnBind,
		HasTag, DomEvents.Change.Handler, DomEvents.Input.Handler,
		LayoutEvents.BeforeRender.Handler, DomEvents.Focusin.Handler,
		DomEvents.Focusout.Handler, DomEvents.KeyDown.Handler {
	private String value;

	private String currentValue;

	private String placeholder;

	private String autocomplete = "off";

	private String type = "text";

	private boolean focusOnBind;

	private String tag = "input";

	private boolean selectAllOnFocus;

	private boolean moveCaretToEndOnFocus;

	private String spellcheck = "false";

	SelectionState selectOnFocus;

	private boolean preserveSelectionOverFocusChange;

	private String rows;

	private boolean ensureAllLinesVisible;

	private boolean commitOnEnter;

	private boolean disabled;

	public StringInput() {
	}

	public StringInput(String value) {
		setValue(value);
	}

	public void clear() {
		setValue("");
	}

	public void copyStateFrom(StringInput input) {
		setValue(input.elementValue());
		selectOnFocus = new SelectionState().snapshot(input.provideElement());
	}

	String elementValue() {
		return provideElement().getPropertyString("value");
	}

	public void focus() {
		FocusImpl.getFocusImplForWidget().focus(provideElement());
	}

	public String getAutocomplete() {
		return this.autocomplete;
	}

	public String getCurrentValue() {
		if (currentValue == null && provideIsBound()) {
			currentValue = elementValue();
		}
		return this.currentValue;
	}

	public String getPlaceholder() {
		return this.placeholder;
	}

	public String getRows() {
		return rows;
	}

	public String getSpellcheck() {
		return this.spellcheck;
	}

	public String getTag() {
		return this.tag;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	public boolean isCommitOnEnter() {
		return commitOnEnter;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public boolean isEnsureAllLinesVisible() {
		return ensureAllLinesVisible;
	}

	@Override
	public boolean isFocusOnBind() {
		return focusOnBind;
	}

	public boolean isMoveCaretToEndOnFocus() {
		return moveCaretToEndOnFocus;
	}

	public boolean isPreserveSelectionOverFocusChange() {
		return this.preserveSelectionOverFocusChange;
	}

	public boolean isSelectAllOnFocus() {
		return this.selectAllOnFocus;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		event.node.optional(Placeholder.class)
				.ifPresent(placeholder -> setPlaceholder(placeholder.value()));
		event.node.optional(FocusOnBind.class)
				.ifPresent(ann -> setFocusOnBind(true));
		super.onBeforeRender(event);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
	}

	@Override
	public void onChange(Change event) {
		currentValue = elementValue();
		setValue(currentValue);
		event.reemitAs(this, ModelEvents.Change.class, currentValue);
	}

	@Override
	public void onFocusin(Focusin event) {
		if (selectOnFocus != null) {
			Scheduler.get().scheduleDeferred(() -> {
				selectOnFocus.apply(provideElement());
				selectOnFocus = null;
			});
		} else if (isSelectAllOnFocus()) {
			Rendered rendered = event.getContext().node.getRendered();
			Element elem = rendered.asElement();
			String propertyString = elem.getPropertyString("value");
			TextBoxImpl.setTextBoxSelectionRange(elem, 0,
					propertyString.length());
		} else if (isMoveCaretToEndOnFocus()) {
			Rendered rendered = event.getContext().node.getRendered();
			Element elem = rendered.asElement();
			String propertyString = elem.getPropertyString("value");
			TextBoxImpl.setTextBoxSelectionRange(elem, propertyString.length(),
					propertyString.length());
		}
	}

	@Override
	public void onFocusout(Focusout event) {
		if (preserveSelectionOverFocusChange && Ax.notBlank(value)) {
			selectOnFocus = new SelectionState();
			selectOnFocus.selectionStart = value.length();
			selectOnFocus.selectionEnd = value.length();
		}
	}

	@Override
	public void onInput(Input event) {
		currentValue = elementValue();
		WidgetUtils.squelchCurrentEvent();
		updateHeight();
		event.reemitAs(this, ModelEvents.Input.class, currentValue);
	}

	@Override
	public void onKeyDown(KeyDown event) {
		Context context = event.getContext();
		KeyDownEvent domEvent = (KeyDownEvent) context.getGwtEvent();
		switch (domEvent.getNativeKeyCode()) {
		case KeyCodes.KEY_ENTER:
			if (commitOnEnter) {
				value = getCurrentValue();
				event.reemitAs(this, Commit.class);
				domEvent.preventDefault();
			}
			// simulate a 'commit' event
			break;
		}
	}

	@Override
	public String provideTag() {
		return getTag();
	}

	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	public void setCommitOnEnter(boolean commitOnEnter) {
		this.commitOnEnter = commitOnEnter;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public void setEnsureAllLinesVisible(boolean ensureAllLinesVisible) {
		this.ensureAllLinesVisible = ensureAllLinesVisible;
	}

	public void setFocusOnBind(boolean focusOnBind) {
		this.focusOnBind = focusOnBind;
	}

	public void setMoveCaretToEndOnFocus(boolean moveCaretToEndOnFocus) {
		this.moveCaretToEndOnFocus = moveCaretToEndOnFocus;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public void setPreserveSelectionOverFocusChange(
			boolean preserveSelectionOverFocusChange) {
		this.preserveSelectionOverFocusChange = preserveSelectionOverFocusChange;
	}

	public void setRows(String rows) {
		this.rows = rows;
	}

	public void setSelectAllOnFocus(boolean selectAllOnBind) {
		this.selectAllOnFocus = selectAllOnBind;
	}

	public void setSpellcheck(String spellcheck) {
		this.spellcheck = spellcheck;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setType(String type) {
		// must set before attach
		Preconditions.checkState(!provideIsBound());
		this.type = type;
	}

	@Override
	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	void updateHeight() {
		if (ensureAllLinesVisible) {
			Scheduler.get().scheduleDeferred(() -> {
				Element element = provideElement();
				element.getStyle().setProperty("height", "auto");
				String paddingTop = WidgetUtils.getComputedStyle(element,
						"paddingTop");
				String paddingBottom = WidgetUtils.getComputedStyle(element,
						"paddingBottom");
				String computedHeight = WidgetUtils.getComputedStyle(element,
						"height");
				double paddingTopPx = paddingTop.endsWith("px")
						? Double.parseDouble(paddingTop.replace("px", ""))
						: 0;
				double paddingBottomPx = paddingBottom.endsWith("px")
						? Double.parseDouble(paddingBottom.replace("px", ""))
						: 0;
				double computedHeightPx = computedHeight.endsWith("px")
						? Double.parseDouble(computedHeight.replace("px", ""))
						: 0;
				int scrollHeight = element.getScrollHeight();
				if (scrollHeight != 0) {
					if (scrollHeight == computedHeightPx + paddingBottomPx
							+ paddingTopPx) {
						// no need to change
					} else {
						element.getStyle().setHeight(scrollHeight,
								// - paddingTopPx - paddingBottomPx,
								Unit.PX);
					}
				}
			});
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface FocusOnBind {
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface Placeholder {
		/**
		 * The placeholder
		 */
		String value();

		public static class Impl implements Placeholder {
			private String value;

			@Override
			public Class<? extends Annotation> annotationType() {
				return Placeholder.class;
			}

			@Override
			public String value() {
				return value;
			}

			public Impl withValue(String value) {
				this.value = value;
				return this;
			}
		}
	}

	static class SelectionState {
		int selectionStart;

		int selectionEnd;

		SelectionState() {
		}

		void apply(Element element) {
			TextBoxImpl.setTextBoxSelectionRange(element, selectionStart,
					selectionEnd - selectionStart);
		}

		SelectionState snapshot(Element element) {
			selectionStart = element.getPropertyInt("selectionStart");
			selectionEnd = element.getPropertyInt("selectionEnd");
			return this;
		}
	}
}