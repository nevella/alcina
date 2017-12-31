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
package cc.alcina.framework.common.client.publication.request;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

/**
 * 
 * @author Nick Reddel
 */
public abstract class PublicationFontOptions extends ExtensibleEnum {
	public static final PublicationFontOptions ARIAL = new PublicationFontOptions_ARIAL();

	public static final PublicationFontOptions TIMES_NEW_ROMAN = new PublicationFontOptions_TIMES_NEW_ROMAN();

	public static final PublicationFontOptions COURIER = new PublicationFontOptions_COURIER();

	public static final PublicationFontOptions GEORGIA = new PublicationFontOptions_GEORGIA();

	public static final PublicationFontOptions ATHELAS = new PublicationFontOptions_ATHELAS();

	public static class PublicationFontOptions_ARIAL
			extends PublicationFontOptions {
	}

	public static class PublicationFontOptions_ATHELAS
			extends PublicationFontOptions {
	}

	public static class PublicationFontOptions_COURIER
			extends PublicationFontOptions {
	}

	public static class PublicationFontOptions_GEORGIA
			extends PublicationFontOptions {
	}

	public static class PublicationFontOptions_TIMES_NEW_ROMAN
			extends PublicationFontOptions {
	}
}
