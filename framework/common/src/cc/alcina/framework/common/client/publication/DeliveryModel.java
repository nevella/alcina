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

import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.publication.Publication.Definition;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;

/**
 *
 * @author Nick Reddel
 */
public interface DeliveryModel extends TreeSerializable {
	public String getAttachmentMessage();

	public String getAttachmentMessageForRequestor();

	public String getEmailAddress();

	public String getEmailSubject();

	public String getEmailSubjectForRequestor();

	/**
	 * The mime type of the content
	 */
	public String getMimeType();

	/**
	 * comma separated fields which indicate the queryString to be put at the
	 * end of a URL. eg link.do?alert,97,a987db34. (link.do? is not included)
	 * first field is always the type.
	 */
	public String getPermalinkQuery();

	public Map<String, String> getProperties();

	public String getSuggestedFileName();

	public String getSystemEmailAddressOfRequestor();

	public boolean isCoverPage();

	public boolean isEmailInline();

	public boolean isFooter();

	public boolean isNoPersistence();

	public boolean isPageBreakAfterEachDocument();

	public boolean isTest();

	public List<MailAttachment> provideAttachments();

	public ContentDeliveryType provideContentDeliveryType();

	public Definition provideDefinition();

	public List<MailInlineImage> provideImages();

	public FormatConversionTarget provideTargetFormat();

	default void addAttachment(MailAttachment attachment) {
		provideAttachments().add(attachment);
	}

	default String getPublicationUid() {
		return null;
	}

	default boolean hasProperty(String keyS) {
		throw new UnsupportedOperationException();
	}

	default String providePropertyValue(String key) {
		throw new UnsupportedOperationException();
	}

	default void removeAttachment(MailAttachment attachment) {
		provideAttachments().remove(attachment);
	}

	public static class MailAttachment {
		public String uid;

		public String contentType;

		public byte[] requestBytes;

		public String dataSourceMimeType;

		public String suggestedFileName;
	}

	public static class MailInlineImage {
		public String uid;

		public String contentType;

		public byte[] requestBytes;

		public String dataSourceMimeType;
	}
}