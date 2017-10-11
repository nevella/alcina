/*
 * Copyright Miroslav Pokorny
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
package rocket.selection.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import rocket.selection.client.support.SelectionSupport;

/**
 * The Selection class is a singleton that represents any selection made by the
 * user typically done with the mouse.
 * 
 * @author Miroslav Pokorny (mP)
 */
public class Selection extends JavaScriptObject {
	/**
	 * The browser aware support that takes care of browser difference nasties.
	 */
	static private SelectionSupport support = (SelectionSupport) GWT
			.create(SelectionSupport.class);

	static SelectionSupport getSupport() {
		return Selection.support;
	}

	/**
	 * Returns the document Selection singleton
	 * 
	 * @return The singleton instance
	 */
	static public Selection getSelection() {
		return Selection.getSelection(BrowserMod.getWindow());
	}

	/**
	 * Returns the document Selection singleton
	 * 
	 * @return The singleton instance
	 */
	static public Selection getSelection(final JavaScriptObject window) {
		return Selection.support.getSelection(window);
	}

	/**
	 * Clears or removes any current text selection.
	 * 
	 */
	public static void clearAnySelectedText() {
		Selection.getSelection().clear();
	}

	protected Selection() {
		super();
	}

	final public SelectionEndPoint getStart() {
		return Selection.getSupport().getStart(this);
	}

	final public void setStart(final SelectionEndPoint start) {
		Selection.getSupport().setStart(this, start);
	}

	final public SelectionEndPoint getEnd() {
		return Selection.getSupport().getEnd(this);
	}

	final public void setEnd(final SelectionEndPoint end) {
		Selection.getSupport().setEnd(this, end);
	}

	/**
	 * Tests if anything is currently being selected
	 * 
	 * @return True if empty false otherwise
	 */
	final public boolean isEmpty() {
		return Selection.getSupport().isEmpty(this);
	}

	/**
	 * Clears any current selection.
	 */
	final public void clear() {
		Selection.getSupport().clear(this);
	}

	/**
	 * Extracts the selection and moves it to become the only child of a new
	 * element.
	 * 
	 * If not selection is active the returned element will have no child / its
	 * innerHTML property will be an empty String.
	 */
	final public Element extract() {
		return Selection.getSupport().extract(this);
	}

	/**
	 * Inserts the given element into the dom so that it is a child of the given
	 * element and yet contains the selected area.
	 * 
	 * This class includes a guard to ensure that a selection exists if not an
	 * exception is thrown.
	 * 
	 * @param element
	 */
	final public void surround(final Element element) {
		Selection.getSupport().surround(this, element);
	}

	/**
	 * Deletes the selection's content from the document.
	 */
	final public void delete() {
		Selection.getSupport().delete(this);
	}
}
