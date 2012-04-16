package cc.alcina.framework.gwt.client.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class WidgetWrapper extends Composite {
	private FlowPanel fp;

	private WidgetElementReplacer replacer;

	public WidgetWrapper() {
		this.fp = new FlowPanel();
		initWidget(fp);
	}

	public interface WidgetElementReplacer {
		public Widget replace(Element elt);
	}

	public void wrap(HTML html, WidgetElementReplacer replacer) {
		this.replacer = replacer;
		Element elt = html.getElement();
		Widget maybeWrap = maybeWrap(elt);
		if (maybeWrap != null) {
			fp.add(maybeWrap);
		} else {
			fp.add(html);
		}
		replaceMap.clear();
	}

	Map<Element, Widget> replaceMap = new HashMap<Element, Widget>();

	private Widget maybeWrap(Element element) {
		Widget replace = replacer.replace(element);
		replaceMap.put(element, replace);
		if (replace != null) {
			return replace;
		}
		List<Element> kids = DomUtils.nodeListToElementList(element
				.getChildNodes());
		boolean hasReplacedChild = false;
		for (Element e2 : kids) {
			if (maybeWrap(e2) != null) {
				hasReplacedChild = true;
			}
		}
		if (hasReplacedChild) {
			com.google.gwt.user.client.Element ctr = DOM.createDiv();
			FlowPanel fp = new FlowPanel();
			for (Element e2 : kids) {
				Widget w = replaceMap.get(e2);
				if (w != null) {
					fp.add(w);
				} else {
					ctr.appendChild(e2);
					fp.add(new HTML(ctr.getInnerHTML()));
					ctr.removeChild(e2);
				}
			}
			replaceMap.put(element, fp);
			fp.setStyleName(element.getClassName());
			return fp;
		}
		return null;
	}
}
