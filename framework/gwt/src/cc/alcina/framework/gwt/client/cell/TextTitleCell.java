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
import com.google.gwt.dom.builder.shared.HtmlSpanBuilder;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

public class TextTitleCell extends AbstractCell<TextTitleTuple> {
	public TextTitleCell() {
	}

	@Override
	public void render(Context context, TextTitleTuple value,
			SafeHtmlBuilder sb) {
		HtmlBuilderFactory factory = HtmlBuilderFactory.get();
		HtmlSpanBuilder builder = factory.createSpanBuilder();
		if (Ax.notBlank(value.title)) {
			builder.title(value.title);
		}
		builder.text(value.text);
		sb.append(builder.asSafeHtml());
	}
}
