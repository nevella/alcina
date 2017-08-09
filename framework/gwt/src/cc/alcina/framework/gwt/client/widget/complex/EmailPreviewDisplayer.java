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

package cc.alcina.framework.gwt.client.widget.complex;


import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.gwt.client.data.EmailPreview;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

/**
 *
 * @author Nick Reddel
 */

 public class EmailPreviewDisplayer extends Composite {
	private Frame frame;

	public Frame getFrame() {
		return this.frame;
	}

	public EmailPreviewDisplayer(EmailPreview model) {
		FlowPanel fp = new FlowPanel();
		Grid g = new Grid(2, 2);
		g.setWidget(0, 0, UsefulWidgetFactory.boldInline("To:"));
		g.setWidget(0, 1, new Label(model.getToAddresses()));
		g.setWidget(1, 0, UsefulWidgetFactory.boldInline("Subject:"));
		g.setWidget(1, 1, new Label(model.getSubject()));
		fp.add(g);
		fp.add(new HTML("<hr />"));
		 frame = new Frame();
		frame.setUrl(model.getBody());
		fp.add(frame);
		initWidget(fp);
	}
}
