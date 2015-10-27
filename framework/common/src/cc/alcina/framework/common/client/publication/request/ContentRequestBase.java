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

import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public abstract class ContentRequestBase<CD extends ContentDefinition> extends
		WrapperPersistable implements GwtMultiplePersistable, DeliveryModel {
	static final long serialVersionUID = -1L;

	private String outputFormat = FormatConversionTarget.HTML.serializedForm();

	private String deliveryMode = ContentDeliveryType.DOWNLOAD.serializedForm();

	private PublicationRange publicationRange = PublicationRange.ALL;

	private String fontOptions = PublicationFontOptions.ARIAL.serializedForm();

	private boolean coverPage = false;

	private String suggestedFileName = "";

	private String emailSubject = "";

	private String emailSubjectForRequestor = null;

	private String attachmentMessage = "";

	private String attachmentMessageForRequestor = "";

	private String systemEmailAddressOfRequestor;

	private boolean pageBreakAfterEachDocument = false;

	private boolean emailInline = true;

	private Long singleContentObjectId;

	private int singleContentObjectResultPosition;

	private int resultCount;

	private transient String systemMessage;

	private String contentPageRange;

	private String emailAddress;

	private String permalinkQuery = null;

	private String mimeType = null;

	private String note;

	private boolean noPersistence = false;

	protected CD contentDefinition;

	private boolean footer = false;

	private Long randomSeed;

	private transient boolean test;

	private String publicDescription;

	private Map<String, String> properties = new LinkedHashMap<String, String>();

	public String getAttachmentMessage() {
		return attachmentMessage;
	}

	public String getAttachmentMessageForRequestor() {
		return attachmentMessageForRequestor;
	}

	/**
	 * Don't assume non-null anywhere along the publication chain - use the
	 * contentDefinition field of the ContentModelHandler etc
	 */
	@XmlTransient
	public abstract CD getContentDefinition();

	public String getContentPageRange() {
		return this.contentPageRange;
	}

	@Display(name = "deliveryMode")
	public String getDeliveryMode() {
		return this.deliveryMode;
	}

	public String getEmailAddress() {
		return emailAddress == null ? PermissionsManager.get().getUser() == null ? null
				: PermissionsManager.get().getUser().getEmail()
				: this.emailAddress;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public String getEmailSubjectForRequestor() {
		if (emailSubjectForRequestor == null) {
			return getEmailSubject();
		}
		return emailSubjectForRequestor;
	}

	public String getFontOptions() {
		return fontOptions;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public String getNote() {
		return note;
	}

	@Display(name = "outputFormat")
	public String getOutputFormat() {
		return this.outputFormat;
	}

	public String getPermalinkQuery() {
		return permalinkQuery;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	@Transient
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return this.propertyChangeSupport().getPropertyChangeListeners();
	}

	public PublicationRange getPublicationRange() {
		return publicationRange;
	}

	public String getPublicDescription() {
		return this.publicDescription;
	}

	public Long getRandomSeed() {
		return this.randomSeed;
	}

	public int getResultCount() {
		return this.resultCount;
	}

	public Long getSingleContentObjectId() {
		return this.singleContentObjectId;
	}

	public int getSingleContentObjectResultPosition() {
		return this.singleContentObjectResultPosition;
	}

	public String getSuggestedFileName() {
		return suggestedFileName;
	}

	public String getSystemEmailAddressOfRequestor() {
		return systemEmailAddressOfRequestor;
	}

	@XmlTransient
	public String getSystemMessage() {
		return systemMessage;
	}

	public boolean hasProperty(String key){
		return properties.containsKey(key);
	}

	public boolean isCoverPage() {
		return coverPage;
	}

	public boolean isEmailInline() {
		return emailInline;
	}

	public boolean isFooter() {
		return footer;
	}

	public boolean isNoPersistence() {
		return noPersistence;
	}

	public boolean isPageBreakAfterEachDocument() {
		return pageBreakAfterEachDocument;
	}

	@XmlTransient
	public boolean isTest() {
		return test;
	}

	@Override
	public ContentDeliveryType provideContentDeliveryType() {
		return ExtensibleEnum.valueOf(ContentDeliveryType.class, deliveryMode);
	}

	public PublicationFontOptions providePublicationFontOptions() {
		return ExtensibleEnum
				.valueOf(PublicationFontOptions.class, fontOptions);
	}

	@Override
	public FormatConversionTarget provideTargetFormat() {
		return ExtensibleEnum.valueOf(FormatConversionTarget.class,
				outputFormat);
	}

	public void putContentDeliveryType(ContentDeliveryType type) {
		setDeliveryMode(type == null ? null : type.name());
	}

	public void putFormatConversionTarget(FormatConversionTarget target) {
		setOutputFormat(target == null ? null : target.name());
	}

	public void putPublicationFontOptions(PublicationFontOptions fontOptions) {
		setFontOptions(fontOptions == null ? null : fontOptions.name());
	}

	public void setAttachmentMessage(String attachmentMessage) {
		this.attachmentMessage = attachmentMessage;
	}

	public void setAttachmentMessageForRequestor(
			String attachmentMessageForRequestor) {
		this.attachmentMessageForRequestor = attachmentMessageForRequestor;
	}

	public abstract void setContentDefinition(CD contentDefinition);

	public void setContentPageRange(String contentPageRange) {
		String old_contentPageRange = this.contentPageRange;
		this.contentPageRange = contentPageRange;
		propertyChangeSupport().firePropertyChange("contentPageRange",
				old_contentPageRange, contentPageRange);
	}

	public void setCoverPage(boolean coverPage) {
		boolean old_coverPage = this.coverPage;
		this.coverPage = coverPage;
		propertyChangeSupport().firePropertyChange("coverPage", old_coverPage,
				coverPage);
	}

	public void setDeliveryMode(String deliveryMode) {
		String old_deliveryMode = this.deliveryMode;
		this.deliveryMode = deliveryMode;
		propertyChangeSupport().firePropertyChange("deliveryMode",
				old_deliveryMode, deliveryMode);
	}

	public void setEmailAddress(String emailAddress) {
		String old_emailAddress = this.emailAddress;
		this.emailAddress = emailAddress;
		propertyChangeSupport().firePropertyChange("emailAddress",
				old_emailAddress, emailAddress);
	}

	public void setEmailInline(boolean emailInline) {
		boolean old_emailInline = this.emailInline;
		this.emailInline = emailInline;
		propertyChangeSupport().firePropertyChange("emailInline",
				old_emailInline, emailInline);
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public void setEmailSubjectForRequestor(String emailSubjectForRequestor) {
		this.emailSubjectForRequestor = emailSubjectForRequestor;
	}

	public void setFontOptions(String fontOptions) {
		String old_fontOptions = this.fontOptions;
		this.fontOptions = fontOptions;
		propertyChangeSupport().firePropertyChange("fontOptions",
				old_fontOptions, fontOptions);
	}

	public void setFooter(boolean footer) {
		boolean old_footer = this.footer;
		this.footer = footer;
		propertyChangeSupport()
				.firePropertyChange("footer", old_footer, footer);
	}

	public void setMimeType(String MimeType) {
		String old_MimeType = this.mimeType;
		propertyChangeSupport().firePropertyChange("MimeType", old_MimeType,
				MimeType);
		this.mimeType = MimeType;
	}

	public void setNoPersistence(boolean noPersistence) {
		this.noPersistence = noPersistence;
	}

	public void setNote(String note) {
		String old_note = this.note;
		this.note = note;
		propertyChangeSupport().firePropertyChange("note", old_note, note);
	}

	public void setOutputFormat(String outputFormat) {
		String old_outputFormat = this.outputFormat;
		this.outputFormat = outputFormat;
		propertyChangeSupport().firePropertyChange("outputFormat",
				old_outputFormat, outputFormat);
	}

	public void setPageBreakAfterEachDocument(boolean pageBreakAfterEachDocument) {
		boolean old_pageBreakAfterEachDocument = this.pageBreakAfterEachDocument;
		this.pageBreakAfterEachDocument = pageBreakAfterEachDocument;
		propertyChangeSupport().firePropertyChange(
				"pageBreakAfterEachDocument", old_pageBreakAfterEachDocument,
				pageBreakAfterEachDocument);
	}

	public void setPermalinkQuery(String permalinkQuery) {
		String old_permalinkQuery = this.permalinkQuery;
		propertyChangeSupport().firePropertyChange("permalinkQuery",
				old_permalinkQuery, permalinkQuery);
		this.permalinkQuery = permalinkQuery;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public void setPublicationRange(PublicationRange publicationRange) {
		PublicationRange old_publicationRange = this.publicationRange;
		this.publicationRange = publicationRange;
		propertyChangeSupport().firePropertyChange("publicationRange",
				old_publicationRange, publicationRange);
	}

	public void setPublicDescription(String publicDescription) {
		this.publicDescription = publicDescription;
	}

	public void setRandomSeed(Long randomSeed) {
		this.randomSeed = randomSeed;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	public void setSingleContentObjectId(Long singleContentObjectId) {
		Long old_singleContentObjectId = this.singleContentObjectId;
		this.singleContentObjectId = singleContentObjectId;
		propertyChangeSupport().firePropertyChange("singleContentObjectId",
				old_singleContentObjectId, singleContentObjectId);
	}

	public void setSingleContentObjectResultPosition(
			int singleContentObjectResultPosition) {
		this.singleContentObjectResultPosition = singleContentObjectResultPosition;
	}

	public void setSuggestedFileName(String suggestedFileName) {
		this.suggestedFileName = suggestedFileName;
	}

	public void setSystemEmailAddressOfRequestor(
			String systemEmailAddressOfRequestor) {
		this.systemEmailAddressOfRequestor = systemEmailAddressOfRequestor;
	}

	public void setSystemMessage(String systemMessage) {
		this.systemMessage = systemMessage;
	}

	public void setTest(boolean test) {
		this.test = test;
	}
	@Override
	public String toString() {
		if (publicDescription != null) {
			return publicDescription;
		}
		String s = contentDefinition == null ? "" : getContentDefinition()
				.toString() + "\n" + "-";
		return s + " Delivery mode: "
				+ CommonUtils.friendlyConstant(getDeliveryMode()) + " - "
				+ " Format: " + CommonUtils.friendlyConstant(getOutputFormat());
	}
}