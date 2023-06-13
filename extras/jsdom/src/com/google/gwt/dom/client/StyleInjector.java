/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * Needless set of optimisations, since handled by localdom batching anyway
 */
public class StyleInjector {
	private static HeadElement head;

	private static List<String> pending = new ArrayList<>();

	private static ScheduledCommand flusher = new ScheduledCommand() {
		@Override
		public void execute() {
			if (needsInjection) {
				flush();
			}
		}
	};

	private static boolean needsInjection = false;

	public static void flush() {
		String contents = pending.stream().collect(Collectors.joining(""));
		StyleElement style = createElement(contents);
		getHead().appendChild(style);
		pending.clear();
		needsInjection = false;
	}

	public static void inject(String contents) {
		pending.add(contents);
		schedule();
	}

	public static void setContents(StyleElement style, String contents) {
		style.setInnerText(contents);
	}

	private static StyleElement createElement(String contents) {
		StyleElement style = Document.get().createStyleElement();
		style.setPropertyString("language", "text/css");
		setContents(style, contents);
		return style;
	}

	private static HeadElement getHead() {
		if (head == null) {
			Element elt = (Element) Document.get().getDocumentElement()
					.asDomNode().children.byTag("head").get(0).domElement();
			assert elt != null : "The host HTML page does not have a <head> element"
					+ " which is required by StyleInjector";
			head = HeadElement.as(elt);
		}
		return head;
	}

	private static void schedule() {
		if (!needsInjection) {
			needsInjection = true;
			Scheduler.get().scheduleFinally(flusher);
		}
	}
}
