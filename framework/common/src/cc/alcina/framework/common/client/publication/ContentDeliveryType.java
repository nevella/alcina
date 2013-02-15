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
package cc.alcina.framework.common.client.publication;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

/**
 * 
 * @author Nick Reddel
 */
public abstract class ContentDeliveryType extends ExtensibleEnum {
	public static final ContentDeliveryType PREVIEW = new ContentDeliveryType_DOWNLOAD_PREVIEW();

	public static final ContentDeliveryType DOWNLOAD = new ContentDeliveryType_DOWNLOAD_ATTACHMENT();

	public static final ContentDeliveryType PRINT = new ContentDeliveryType_PRINT();

	public static final ContentDeliveryType EMAIL = new ContentDeliveryType_EMAIL();

	public static final ContentDeliveryType SEND_TO_REPOSITORY = new ContentDeliveryType_SEND_TO_REPOSITORY();

	public static final ContentDeliveryType PERMALINK = new ContentDeliveryType_PERMALINK();

	public static class ContentDeliveryType_DOWNLOAD_PREVIEW extends
			ContentDeliveryType {
		@Override
		public String serializedForm() {
			return "PREVIEW";
		}
	}

	public static class ContentDeliveryType_DOWNLOAD_ATTACHMENT extends
			ContentDeliveryType {
		@Override
		public String serializedForm() {
			return "DOWNLOAD";
		}
	}

	// note, no handler for this - it's pushed to the client earlier in the
	// publish cycle
	public static class ContentDeliveryType_PRINT extends ContentDeliveryType {
	}

	public static class ContentDeliveryType_EMAIL extends ContentDeliveryType {
	}

	public static class ContentDeliveryType_PERMALINK extends
			ContentDeliveryType {
		@Override
		public boolean isRepublishable() {
			return false;
		}
	}

	public static class ContentDeliveryType_SEND_TO_REPOSITORY extends
			ContentDeliveryType {
	}

	public boolean isRepublishable() {
		return true;
	}
}
