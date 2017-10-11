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
package rocket.selection.client.support;

import rocket.selection.client.Selection;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A specialised SelectionSupport class that is adapted to handle Opera
 * differences from the standard implementation.
 * 
 * @author Miroslav Pokorny (mP)
 */
public class OperaSelectionSupport extends SelectionSupport {
	@Override
	native public Selection getSelection(final JavaScriptObject window)/*-{
        return window.getSelection();
	}-*/;
}
