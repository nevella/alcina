package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.gwittir.widget.FileData;
import cc.alcina.framework.gwt.client.gwittir.widget.Html5File;

/**
 * <p>
 * This class models an editable boolean field, rendering as a Checkbox DOM
 * element.
 *
 *
 * <p>
 * It fires the <code>&lt;Change&gt;</code> model event , wrapping the
 * corresponding DOM event
 *
 *
 *
 *
 */
@Directed(tag = "input", emits = { ModelEvents.Change.class })
@Bean(PropertySource.FIELDS)
@Registration({ Model.Value.class, FormModel.Editor.class, FileData.class })
public class FileInput extends Model.Value<FileData>
		implements DomEvents.Change.Handler {
	public static final transient String VALUE = "value";

	@Binding(type = Type.PROPERTY)
	final String type = "file";

	FileData value;

	@Binding(type = Type.PROPERTY)
	public String accept;

	@Binding(type = Type.PROPERTY)
	public boolean multiple;

	public FileInput() {
	}

	boolean elementValue() {
		return provideElement().getPropertyBoolean("checked");
	}

	native JsArray<Html5File> getFiles(Element element) /*-{
    var remote = element.@com.google.gwt.dom.client.Element::jsoRemote()();
    return remote.files;
	}-*/;

	@Override
	public FileData getValue() {
		return this.value;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		event.node.optional(Accept.class)
				.ifPresent(accept -> this.accept = accept.value());
		super.onBeforeRender(event);
	}

	@Override
	public void onChange(Change event) {
		JsArray<Html5File> files = getFiles(provideElement());
		Preconditions.checkState(files.length() <= 1);
		if (files.length() == 1) {
			FileData.fromFile(files.get(0), fileData -> {
				setValue(fileData);
				event.reemitAs(this, ModelEvents.Change.class);
			});
		}
	}

	@Override
	public void setValue(FileData value) {
		set(VALUE, this.value, value, () -> this.value = value);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface Accept {
		/**
		 * The accept pattern (e.g. *.txt) for the DOM file input
		 */
		String value();
	}
}