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

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Frame;

/**
 *
 * @author Nick Reddel
 */
public class FrameX extends Frame {
	public native Document getDocument()/*-{
	return null;
	}-*/;

	/**
	 * Should not include '<html>', '</html>' tags
	 */
	public void setDocumentHtml(String html) {
		this.html = html;
		if (isAttached()) {
			_setDocumentHtml(html);
		}
	}

	private String html;

	@Override
	protected void onAttach() {
		super.onAttach();
		if (html != null) {
			_setDocumentHtml(html);
		}
	}

	private native void _setDocumentHtml(String html)/*-{
	var elt = this.@cc.alcina.framework.gwt.client.widget.complex.FrameX::getElement()();
	var oDoc = elt.contentWindow || elt.contentDocument;
	if (oDoc.document) {
		oDoc = oDoc.document;
	}

	//Trigger a page "load" (ff issue)
	oDoc.open();
	oDoc.close();
	oDoc.documentElement.innerHTML = html;

	}-*/;
}
