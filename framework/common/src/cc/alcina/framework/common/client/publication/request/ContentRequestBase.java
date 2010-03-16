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

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.DELIVERY_DOWNLOAD_ATTACHMENT;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.DELIVERY_DOWNLOAD_PREVIEW;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.DELIVERY_EMAIL;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.DELIVERY_PRINT;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_DOC;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_DOCX;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_HTML;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_PDF;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_XLS;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_XLSX;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_ZIP;
import cc.alcina.framework.common.client.util.CommonUtils;


/**
 *
 * @author Nick Reddel
 */

 public abstract class ContentRequestBase<CD extends ContentDefinition> extends
		GwtPersistableObject implements GwtMultiplePersistable, DeliveryModel {
	static final long serialVersionUID = -1L;
	private PublicationOutputFormat outputFormat = PublicationOutputFormat.HTML;
	private PublicationDeliveryMode deliveryMode = PublicationDeliveryMode.DOWNLOAD;
	private PublicationRange publicationRange = PublicationRange.ALL;
	private PublicationFontOptions fontOptions = PublicationFontOptions.ARIAL;
	private boolean coverPage = true;
	private String suggestedFileName = "";
	private String emailSubject = "";
	private String emailSubjectForRequestor = null;
	private String attachmentMessage = "";
	private String attachmentMessageForRequestor = "";
	private String systemEmailAddressOfRequestor;
	private boolean pageBreakAfterEachDocument = false;
	private boolean emailInline=true;
	private Long singleContentObjectId;
	private int singleContentObjectResultPosition;
	private int resultCount;
	private transient String systemMessage;
	private String contentPageRange;
	private String emailAddress;
	private String note;
	private boolean noPersistence = false;
	protected CD contentDefinition;
	private boolean footer = true;
	private transient boolean test;

	public Class<? extends ContentDeliveryType> deliveryMode() {
		if (deliveryMode == null) {
			return null;
		}
		switch (deliveryMode) {
		case DOWNLOAD:
			return DELIVERY_DOWNLOAD_ATTACHMENT.class;
		case EMAIL:
			return DELIVERY_EMAIL.class;
		case PREVIEW:
			return DELIVERY_DOWNLOAD_PREVIEW.class;
		case PRINT:
			return DELIVERY_PRINT.class;
		default:
			return null;
		}
	}

	public String getAttachmentMessage() {
		return attachmentMessage;
	}

	public String getAttachmentMessageForRequestor() {
		return attachmentMessageForRequestor;
	}

	@XmlTransient
	public abstract CD getContentDefinition();

	public String getContentPageRange() {
		return this.contentPageRange;
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "deliveryMode"))
	public PublicationDeliveryMode getDeliveryMode() {
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

	public PublicationFontOptions getFontOptions() {
		return fontOptions;
	}

	public String getNote() {
		return note;
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "outputFormat"))
	public PublicationOutputFormat getOutputFormat() {
		return this.outputFormat;
	}

	@Transient
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return this.propertyChangeSupport.getPropertyChangeListeners();
	}

	public PublicationRange getPublicationRange() {
		return publicationRange;
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

	public void setAttachmentMessage(String attachmentMessage) {
		this.attachmentMessage = attachmentMessage;
	}

	public void setAttachmentMessageForRequestor(String attachmentMessageForRequestor) {
		this.attachmentMessageForRequestor = attachmentMessageForRequestor;
	}

	public abstract void setContentDefinition(CD contentDefinition);

	public void setContentPageRange(String contentPageRange) {
		String old_contentPageRange = this.contentPageRange;
		this.contentPageRange = contentPageRange;
		propertyChangeSupport.firePropertyChange("contentPageRange",
				old_contentPageRange, contentPageRange);
	}

	public void setCoverPage(boolean coverPage) {
		boolean old_coverPage = this.coverPage;
		this.coverPage = coverPage;
		propertyChangeSupport.firePropertyChange("coverPage", old_coverPage,
				coverPage);
	}

	public void setDeliveryMode(PublicationDeliveryMode deliveryMode) {
		PublicationDeliveryMode old_deliveryMode = this.deliveryMode;
		this.deliveryMode = deliveryMode;
		propertyChangeSupport.firePropertyChange("deliveryMode",
				old_deliveryMode, deliveryMode);
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public void setEmailInline(boolean emailInline) {
		boolean old_emailInline = this.emailInline;
		this.emailInline = emailInline;
		propertyChangeSupport.firePropertyChange("emailInline",
				old_emailInline, emailInline);
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public void setEmailSubjectForRequestor(String emailSubjectForRequestor) {
		this.emailSubjectForRequestor = emailSubjectForRequestor;
	}

	public void setFontOptions(PublicationFontOptions fontOptions) {
		this.fontOptions = fontOptions;
	}

	public void setFooter(boolean footer) {
		boolean old_footer = this.footer;
		this.footer = footer;
		propertyChangeSupport.firePropertyChange("footer", old_footer, footer);
	}

	public void setNoPersistence(boolean noPersistence) {
		this.noPersistence = noPersistence;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public void setOutputFormat(PublicationOutputFormat outputFormat) {
		PublicationOutputFormat old_outputFormat = this.outputFormat;
		this.outputFormat = outputFormat;
		propertyChangeSupport.firePropertyChange("outputFormat",
				old_outputFormat, outputFormat);
	}

	public void setPageBreakAfterEachDocument(boolean pageBreakAfterEachDocument) {
		boolean old_pageBreakAfterEachDocument = this.pageBreakAfterEachDocument;
		this.pageBreakAfterEachDocument = pageBreakAfterEachDocument;
		propertyChangeSupport.firePropertyChange("pageBreakAfterEachDocument",
				old_pageBreakAfterEachDocument, pageBreakAfterEachDocument);
	}

	public void setPublicationRange(PublicationRange publicationRange) {
		this.publicationRange = publicationRange;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	public void setSingleContentObjectId(Long singleContentObjectId) {
		Long old_singleContentObjectId = this.singleContentObjectId;
		this.singleContentObjectId = singleContentObjectId;
		propertyChangeSupport.firePropertyChange("singleContentObjectId",
				old_singleContentObjectId, singleContentObjectId);
	}

	public void setSingleContentObjectResultPosition(int singleContentObjectResultPosition) {
		this.singleContentObjectResultPosition = singleContentObjectResultPosition;
	}

	public void setSuggestedFileName(String suggestedFileName) {
		this.suggestedFileName = suggestedFileName;
	}

	public void setSystemEmailAddressOfRequestor(String systemEmailAddressOfRequestor) {
		this.systemEmailAddressOfRequestor = systemEmailAddressOfRequestor;
	}

	public void setSystemMessage(String systemMessage) {
		this.systemMessage = systemMessage;
	}

	public Class<? extends FormatConversionTarget> targetFormat() {
		switch (outputFormat) {
		case DOCX:
			return FMT_DOCX.class;
		case DOC:
			return FMT_DOC.class;
		case PDF:
			return FMT_PDF.class;
		case HTML:
			return FMT_HTML.class;
		case XLSX:
			return FMT_XLSX.class;
		case XLS:
			return FMT_XLS.class;
		case ZIP:
			return FMT_ZIP.class;
		default:
			return null;
		}
	}

	@Override
	public String toString() {
		String s = contentDefinition == null ? "" : getContentDefinition()
				.toString()
				+ "\n" + "-";
		return s + " Delivery mode: "
				+ CommonUtils.friendlyConstant(getDeliveryMode()) + " - "
				+ " Format: " + CommonUtils.friendlyConstant(getOutputFormat());
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	@XmlTransient
	public boolean isTest() {
		return test;
	}
}