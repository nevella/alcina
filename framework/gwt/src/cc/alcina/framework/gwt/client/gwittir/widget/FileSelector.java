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
package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasEnabled;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

/**
 * A very, very, very simple, rapid implementation. Very.
 * 
 * @author Nick Reddel
 */
public class FileSelector extends AbstractBoundWidget<FileSelectorInfo>
		implements ChangeHandler, HasEnabled {
	private FileInput base;

	private FileSelectorInfo value;

	private TopicListener<FileSelectorInfo> clearListener = (k, v) -> base
			.clear();

	public FileSelector() {
		this.base = new FileInput();
		initWidget(base);
		base.addChangeHandler(this);
	}

	public FileSelectorInfo getValue() {
		return this.value;
	}

	@Override
	public boolean isEnabled() {
		return base.isEnabled();
	}

	@Override
	public void onChange(ChangeEvent event) {
		Html5File[] files = base.getFiles();
		if (files.length == 1) {
			Html5File file = files[0];
			final FileSelectorInfo newInfo = new FileSelectorInfo();
			newInfo.setFileName(file.getFileName());
			readAsBinaryString(file, new AsyncCallback<String>() {
				@Override
				public void onFailure(Throwable caught) {
					throw new WrappedRuntimeException(caught);
				}

				@Override
				public void onSuccess(String result) {
					int length = result.length();
					byte[] bytes = new byte[length];
					for (int i = 0; i < length; i++) {
						bytes[i] = (byte) result.charAt(i);
					}
					newInfo.setBytes(bytes);
					newInfo.topicClear().add(clearListener);
					FileSelectorInfo oldValue = value;
					if (oldValue != null) {
						oldValue.topicClear().remove(clearListener);
					}
					value = newInfo;
					changes.firePropertyChange("value", oldValue, newInfo);
				}
			});
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		base.setEnabled(enabled);
	}

	// never actually called - 'value' only created post readBinaryString
	public void setValue(FileSelectorInfo value) {
		this.value = value;
	}

	private native void readAsBinaryString(Html5File file,
			AsyncCallback<String> callback)/*-{
    var reader = new FileReader();
    reader.onloadend = function() {
      callback.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(reader.result);

    };
    reader.readAsBinaryString(file);
	}-*/;
}
