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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.widget.Link;

/**
 *
 * @author Nick Reddel
 */
public class ExpandableLabel extends AbstractBoundWidget {
	private FlowPanel fp;

	private boolean showNewlinesAsBreaks;

	private boolean showAsPopup;

	private boolean showHideAtEnd;

	private Link hideLink;

	private boolean hiding;

	private Renderer renderer;

	private final int maxLength;

	List<Widget> hiddenWidgets = new ArrayList<Widget>();

	private InlineLabel dots;

	private InlineHTML space;

	private Link showLink;

	private boolean escapeHtml;

	ClickHandler showHideListener = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			Widget sender = (Widget) event.getSource();
			hiding = !hiding;
			if (showAsPopup) {
				if (!hiding) {
					ScrollPanel sp = new ScrollPanel();
					String popupText = escapeHtml
							? SafeHtmlUtils.htmlEscape(fullTextNoBrs)
							: fullTextNoBrs;
					Label label = new InlineHTML(popupText);
					sp.add(label);
					sp.setStyleName("alcina-expandable-label-popup");
					ClientNotifications.get().setDialogAnimationEnabled(false);
					ClientNotifications.get().showMessage(sp);
					ClientNotifications.get().setDialogAnimationEnabled(true);
				}
				hiding = true;
			} else {
				for (Widget w : hiddenWidgets) {
					w.setVisible(!hiding);
				}
				hideLink.setVisible(!hiding);
				space.setVisible(!hiding);
				showLink.setVisible(hiding);
				dots.setVisible(hiding);
			}
		}
	};

	private String fullText;

	private String fullTextNoBrs;

	public ExpandableLabel(int maxLength) {
		this.maxLength = maxLength;
		this.fp = new FlowPanel();
		initWidget(fp);
		fp.setStyleName("alcina-expandableLabel");
	}

	public Renderer getRenderer() {
		return this.renderer;
	}

	@Override
	public Object getValue() {
		return null;
	}

	public boolean isEscapeHtml() {
		return this.escapeHtml;
	}

	public boolean isShowAsPopup() {
		return this.showAsPopup;
	}

	public boolean isShowHideAtEnd() {
		return showHideAtEnd;
	}

	public boolean isShowNewlinesAsBreaks() {
		return this.showNewlinesAsBreaks;
	}

	public void setEscapeHtml(boolean escapeHtml) {
		this.escapeHtml = escapeHtml;
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	public void setShowAsPopup(boolean showAsPopup) {
		this.showAsPopup = showAsPopup;
	}

	public void setShowHideAtEnd(boolean showHideAtEnd) {
		this.showHideAtEnd = showHideAtEnd;
	}

	public void setShowNewlinesAsBreaks(boolean showNewlinesAsBreaks) {
		this.showNewlinesAsBreaks = showNewlinesAsBreaks;
	}

	@Override
	public void setValue(Object o) {
		fp.clear();
		if (o == null) {
			// fp.add(new InlineLabel("[Undefined]"));
			return;
		}
		boolean hidden = false;
		if (o instanceof Collection) {
			ArrayList l = new ArrayList((Collection) o);
			if (l.size() > 0 && l.get(0) instanceof Comparable) {
				Collections.sort(l);
			}
			int strlen = 0;
			for (Object object : l) {
				InlineLabel comma = new InlineLabel(", ");
				if (strlen > 0) {
					fp.add(comma);
					strlen += 2;
				}
				String name = ClientReflector.get()
						.displayNameForObject(object);
				InlineLabel label = new InlineLabel(name);
				if (strlen > getMaxLength()) {
					comma.setVisible(false);
					label.setVisible(false);
					hiddenWidgets.add(comma);
					hiddenWidgets.add(label);
					hidden = true;
				}
				strlen += name.length();
				fp.add(label);
			}
		} else {
			fullText = renderer == null ? o.toString()
					: renderer.render(o).toString();
			fullTextNoBrs = fullText;
			if (isShowNewlinesAsBreaks()) {
				fullText = SafeHtmlUtils.htmlEscape(fullText).replace("\n",
						"<br>\n");
			}
			int maxC = getMaxLength();
			int y1 = fullText.indexOf(">", maxC);
			int y2 = fullText.indexOf("<", maxC);
			int y3 = fullText.indexOf("<");
			if (y1 < y2 && y1 != -1 && y3 < maxC && !escapeHtml) {
				maxC = y1 + 1;
			}
			String vis = CommonUtils.trimToWsChars(fullText, maxC);
			com.google.gwt.user.client.ui.Label label;
			if (fullText.length() == vis.length()) {
				label = isShowNewlinesAsBreaks() ? new InlineHTML(fullText)
						: new InlineLabel(fullText);
				fp.add(label);
			} else {
				label = isShowNewlinesAsBreaks() ? new InlineHTML(vis)
						: new InlineLabel(vis);
				fp.add(label);
				hidden = true;
				if (!isShowAsPopup()) {
					String notVis = fullText.substring(vis.length());
					label = isShowNewlinesAsBreaks() ? new InlineHTML(notVis)
							: new InlineLabel(notVis);
					label.setVisible(false);
					fp.add(label);
					hiddenWidgets.add(label);
				}
			}
		}
		if (hidden) {
			this.dots = new InlineLabel("...");
			this.space = new InlineHTML("&nbsp;");
			fp.add(dots);
			this.hiding = true;
			this.showLink = new Link("[more]");
			this.hideLink = new Link("[less]");
			if (isShowHideAtEnd()) {
				fp.add(space);
				fp.add(hideLink);
			} else {
				fp.insert(hideLink, 0);
				fp.add(space);
			}
			hideLink.setVisible(false);
			space.setVisible(false);
			fp.add(showLink);
			hideLink.addClickHandler(showHideListener);
			showLink.addClickHandler(showHideListener);
		}
	}

	private int getMaxLength() {
		return maxLength;
	}
}