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
import com.google.gwt.user.client.ui.HasEnabled;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.util.TopicListener;

/**
 * A very, very, very simple, rapid implementation. Very.
 *
 * @author Nick Reddel
 */
public class FileSelector extends AbstractBoundWidget<FileData>
		implements ChangeHandler, HasEnabled {
	private FileInput base;

	private FileData value;

	private String accept;

	private TopicListener<FileData> clearListener = v -> base.clear();

	public FileSelector() {
		this.base = new FileInput();
		initWidget(base);
		base.addChangeHandler(this);
	}

	public String getAccept() {
		return this.accept;
	}

	@Override
	public FileData getValue() {
		return this.value;
	}

	@Override
	public boolean isEnabled() {
		return base.isEnabled();
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		base.setAccept(accept);
	}

	@Override
	public void onChange(ChangeEvent event) {
		updateValue(true);
	}

	// this code is to handle webdriver not firing a change event when the input
	// is modified via sendKeys (otherwise it'd just be in the onChange handler)
	private void updateValue(boolean fire) {
		Html5File[] files = base.getFiles();
		if (files.length == 1) {
			Html5File file = files[0];
			FileData.fromFile(file, fileData -> {
				fileData.topicClear().add(clearListener);
				FileData oldValue = this.value;
				if (oldValue != null) {
					oldValue.topicClear().remove(clearListener);
				}
				this.value = fileData;
				if (fire) {
					changes.firePropertyChange("value", oldValue, fileData);
				}
			});
		}
	}

	public void setAccept(String accept) {
		this.accept = accept;
	}

	@Override
	public void setEnabled(boolean enabled) {
		base.setEnabled(enabled);
	}

	// never actually called - 'value' only created post readBinaryString
	@Override
	public void setValue(FileData value) {
		this.value = value;
	}
}
