/*
 * Copyright 2010 Google Inc.
 *
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
package cc.alcina.framework.gwt.client.cell;

import static com.google.gwt.dom.client.BrowserEvents.*;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.builder.shared.HtmlAnchorBuilder;
import com.google.gwt.dom.builder.shared.HtmlBuilderFactory;
import com.google.gwt.dom.builder.shared.HtmlElementBuilderBase;
import com.google.gwt.dom.builder.shared.HtmlSpanBuilder;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

public class FunctionalCell extends AbstractCell<FunctionalTuple> {
	public FunctionalCell() {
		super(CLICK, KEYDOWN);
	}

	@Override
	public void onBrowserEvent(Context context, Element parent,
			FunctionalTuple value, NativeEvent event,
			ValueUpdater<FunctionalTuple> valueUpdater) {
		super.onBrowserEvent(context, parent, value, event, valueUpdater);
		if (CLICK.equals(event.getType())) {
			EventTarget eventTarget = event.getEventTarget();
			if (!Element.is(eventTarget)) {
				return;
			}
			if (parent.getFirstChildElement()
					.isOrHasChild(Element.as(eventTarget))) {
				event.stopPropagation();
			}
		}
	}

	@Override
	public void render(Context context, FunctionalTuple value,
			SafeHtmlBuilder sb) {
		HtmlBuilderFactory factory = HtmlBuilderFactory.get();
		HtmlElementBuilderBase builderBase = null;
		if (value.href != null) {
			HtmlAnchorBuilder builder = factory.createAnchorBuilder();
			builderBase = builder;
			builder.href(value.href);
		} else if (value.place != null) {
			HtmlAnchorBuilder builder = factory.createAnchorBuilder();
			builderBase = builder;
			builder.href(
					"#" + RegistryHistoryMapper.get().getToken(value.place));
		} else {
			HtmlSpanBuilder builder = factory.createSpanBuilder();
			builderBase = builder;
		}
		if (value.title != null) {
			builderBase.title(value.title);
		}
		builderBase.text(value.text);
		sb.append(builderBase.asSafeHtml());
	}
}
