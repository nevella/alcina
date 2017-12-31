package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Widget;

public class FileInput extends Widget
		implements HasName, HasChangeHandlers, HasEnabled {
	private FileInputImpl impl;

	private InputElement inputElement;

	public FileInput() {
		inputElement = Document.get().createFileInputElement();
		setElement(inputElement);
		setStyleName("gwt-FileUpload");
		impl = new FileInputImplHtml5();
	}

	@Override
	public HandlerRegistration addChangeHandler(ChangeHandler handler) {
		return addDomHandler(handler, ChangeEvent.getType());
	}

	public Html5File[] getFiles() {
		JsArray<Html5File> files = impl.getFiles(inputElement);
		Html5File[] result = new Html5File[files.length()];
		for (int i = 0; i < files.length(); ++i) {
			result[i] = files.get(i);
		}
		return result;
	}

	public String getName() {
		return inputElement.getName();
	}

	public boolean isAllowedMultipleFiles() {
		return impl.isAllowMultipleFiles(inputElement);
	}

	@Override
	public boolean isEnabled() {
		return !inputElement.isDisabled();
	}

	public void setAllowMultipleFiles(boolean allow) {
		impl.setAllowMultipleFiles(inputElement, allow);
	}

	@Override
	public void setEnabled(boolean enabled) {
		inputElement.setDisabled(!enabled);
	}

	public void setName(String name) {
		inputElement.setName(name);
	}

	public boolean supportsFileAPI() {
		return impl.supportsFileAPI();
	}

	private static class FileInputImpl {
		public native JsArray<Html5File>
				getFiles(InputElement inputElement) /*-{
													var remote = inputElement.@com.google.gwt.dom.client.Element::typedRemote()();
													return remote.value && remote.value!=""?
													[{fileName: remote.value, fileSize: -1}]:
													[];
													}-*/;

		public boolean isAllowMultipleFiles(InputElement inputElement) {
			return false;
		}

		public void setAllowMultipleFiles(InputElement inputElement,
				boolean allow) {
		}

		public boolean supportsFileAPI() {
			return false;
		}
	}

	private static class FileInputImplHtml5 extends FileInputImpl {
		@Override
		public native JsArray<Html5File>
				getFiles(InputElement inputElement) /*-{
													var remote = inputElement.@com.google.gwt.dom.client.Element::typedRemote()();
													return remote.files;
													}-*/;

		@Override
		public boolean isAllowMultipleFiles(InputElement inputElement) {
			return inputElement.hasAttribute("multiple");
		}

		@Override
		public void setAllowMultipleFiles(InputElement inputElement,
				boolean allow) {
			if (allow) {
				inputElement.setAttribute("multiple", "");
			} else {
				inputElement.removeAttribute("multiple");
			}
		}

		@Override
		public boolean supportsFileAPI() {
			return true;
		}
	}
}
