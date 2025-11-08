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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.publication.Publication.Definition;
import cc.alcina.framework.common.client.repository.RepositoryConnection;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

/**
 * @author Nick Reddel
 */
public abstract class ContentRequestBase<CD extends ContentDefinition> extends
		Bindable implements DeliveryModel, TreeSerializable, Definition {
	public static class TestContentDefinition implements ContentDefinition {
		@Override
		public String getPublicationType() {
			return "test";
		}
	}

	@TypeSerialization(flatSerializable = false)
	public static class TestContentRequest
			extends ContentRequestBase<TestContentDefinition> {
		@Override
		public TestContentDefinition getContentDefinition() {
			return this.contentDefinition;
		}

		@Override
		public void
				setContentDefinition(TestContentDefinition contentDefinition) {
			this.contentDefinition = contentDefinition;
		}
	}

	private static class Customiser
			extends TreeSerializable.Customiser<ContentRequestBase> {
		public Customiser(ContentRequestBase serializable) {
			super(serializable);
		}

		@Override
		public void onAfterTreeDeserialize() {
			StringMap.fromPropertyString(serializable.propertiesSerialized)
					.forEach((k, v) -> serializable.properties.put(k, v));
			if (serializable.contentDefinition != null) {
				serializable.contentDefinition.treeSerializationCustomiser()
						.onAfterTreeDeserialize();
			}
		}

		@Override
		public void onAfterTreeSerialize() {
			serializable.properties = new LinkedHashMap<>();
			StringMap.fromPropertyString(serializable.propertiesSerialized)
					.forEach((k, v) -> serializable.properties.put(k, v));
			if (serializable.contentDefinition != null) {
				serializable.contentDefinition.treeSerializationCustomiser()
						.onAfterTreeSerialize();
			}
		}

		@Override
		public void onBeforeTreeDeserialize() {
			if (serializable.contentDefinition != null) {
				serializable.contentDefinition.treeSerializationCustomiser()
						.onBeforeTreeDeserialize();
			}
		}

		@Override
		public void onBeforeTreeSerialize() {
			if (serializable.properties == null) {
				serializable.properties = new LinkedHashMap<>();
			}
			serializable.propertiesSerialized = new StringMap(
					serializable.properties).toPropertyString();
			serializable.properties = new LinkedHashMap<>();
			if (serializable.contentDefinition != null) {
				serializable.contentDefinition.treeSerializationCustomiser()
						.onBeforeTreeSerialize();
			}
		}
	}

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

	private Map<String, String> properties = new LinkedHashMap<>();

	private String propertiesSerialized = "";

	public transient List<MailInlineImage> images = new ArrayList<>();

	public transient List<MailAttachment> attachments = new ArrayList<>();

	private String opaqueRequestXml;

	private String opaqueRequestClassname;

	private List<MultipleDeliveryEntry> multipleDeliveryEntries = new ArrayList<>();

	private RepositoryConnection repositoryConnection = new RepositoryConnection();

	private Long requestorClientInstanceId;

	public Long getRequestorClientInstanceId() {
		return requestorClientInstanceId;
	}

	public void setRequestorClientInstanceId(Long requestorClientInstanceId) {
		this.requestorClientInstanceId = requestorClientInstanceId;
	}

	public void ensureSuggestedFilename(String filename) {
		if (Ax.isBlank(getSuggestedFileName())) {
			setSuggestedFileName(filename);
		}
	}

	@Override
	public String getAttachmentMessage() {
		return attachmentMessage;
	}

	@Override
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

	@Override
	public String getEmailAddress() {
		return emailAddress == null
				? Permissions.get().getUser() == null ? null
						: Permissions.get().getUser().getEmail()
				: this.emailAddress;
	}

	@Override
	public String getEmailSubject() {
		return emailSubject;
	}

	@Override
	public String getEmailSubjectForRequestor() {
		if (Ax.isBlank(emailSubjectForRequestor)) {
			return getEmailSubject();
		}
		return emailSubjectForRequestor;
	}

	public String getFontOptions() {
		return fontOptions;
	}

	@Override
	public String getMimeType() {
		return this.mimeType;
	}

	@Override
	public List<MultipleDeliveryEntry> getMultipleDeliveryEntries() {
		return this.multipleDeliveryEntries;
	}

	public String getNote() {
		return note;
	}

	public String getOpaqueRequestClassname() {
		return this.opaqueRequestClassname;
	}

	/**
	 * For when we don't want the client to have to know the request class
	 */
	public String getOpaqueRequestXml() {
		return this.opaqueRequestXml;
	}

	@Display(name = "outputFormat")
	public String getOutputFormat() {
		return this.outputFormat;
	}

	@Override
	public String getPermalinkQuery() {
		return permalinkQuery;
	}

	@Override
	@PropertySerialization(ignoreFlat = true)
	public Map<String, String> getProperties() {
		return this.properties;
	}

	@PropertySerialization(notTestable = true)
	public String getPropertiesSerialized() {
		return this.propertiesSerialized;
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

	@Override
	public RepositoryConnection getRepositoryConnection() {
		return this.repositoryConnection;
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

	@Override
	public String getSuggestedFileName() {
		return suggestedFileName;
	}

	@Override
	public String getSystemEmailAddressOfRequestor() {
		return systemEmailAddressOfRequestor;
	}

	@XmlTransient
	public String getSystemMessage() {
		return systemMessage;
	}

	@Override
	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	@Override
	public boolean isCoverPage() {
		return coverPage;
	}

	@Override
	public boolean isEmailInline() {
		return emailInline;
	}

	@Override
	public boolean isFooter() {
		return footer;
	}

	@Override
	public boolean isNoPersistence() {
		return noPersistence;
	}

	@Override
	public boolean isPageBreakAfterEachDocument() {
		return pageBreakAfterEachDocument;
	}

	@Override
	@XmlTransient
	public boolean isTest() {
		return test;
	}

	@Override
	@Transient
	@AlcinaTransient
	public PropertyChangeListener[] propertyChangeListeners() {
		return this.propertyChangeSupport().getPropertyChangeListeners();
	}

	@Override
	public List<MailAttachment> provideAttachments() {
		if (attachments == null) {
			attachments = new ArrayList<>();
		}
		return attachments;
	}

	@Override
	public ContentDefinition provideContentDefinition() {
		return contentDefinition;
	}

	@Override
	public ContentDeliveryType provideContentDeliveryType() {
		return ExtensibleEnum.valueOf(ContentDeliveryType.class, deliveryMode);
	}

	@Override
	public Definition provideDefinition() {
		return this;
	}

	@Override
	public DeliveryModel provideDeliveryModel() {
		return this;
	}

	@Override
	public List<MailInlineImage> provideImages() {
		if (images == null) {
			images = new ArrayList<>();
		}
		return images;
	}

	public String provideJobName() {
		return getClass().getSimpleName();
	}

	@Override
	public String providePropertyValue(String key) {
		return properties.get(key);
	}

	public PublicationFontOptions providePublicationFontOptions() {
		return ExtensibleEnum.valueOf(PublicationFontOptions.class,
				fontOptions);
	}

	@Override
	public FormatConversionTarget provideTargetFormat() {
		return ExtensibleEnum.valueOf(FormatConversionTarget.class,
				outputFormat);
	}

	public PublicationResult publish() {
		try {
			return PublicationRequestHandler.get().publish(this);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public PublicationResult publishUnchecked() {
		try {
			return publish();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
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
		propertyChangeSupport().firePropertyChange("footer", old_footer,
				footer);
	}

	public void setMimeType(String MimeType) {
		String old_MimeType = this.mimeType;
		propertyChangeSupport().firePropertyChange("MimeType", old_MimeType,
				MimeType);
		this.mimeType = MimeType;
	}

	public void setMultipleDeliveryEntries(
			List<MultipleDeliveryEntry> multipleDeliveryEntries) {
		this.multipleDeliveryEntries = multipleDeliveryEntries;
	}

	public void setNoPersistence(boolean noPersistence) {
		this.noPersistence = noPersistence;
	}

	public void setNote(String note) {
		String old_note = this.note;
		this.note = note;
		propertyChangeSupport().firePropertyChange("note", old_note, note);
	}

	public void setOpaqueRequestClassname(String opaqueRequestClassname) {
		this.opaqueRequestClassname = opaqueRequestClassname;
	}

	public void setOpaqueRequestXml(String opaqueRequestXml) {
		this.opaqueRequestXml = opaqueRequestXml;
	}

	public void setOutputFormat(String outputFormat) {
		String old_outputFormat = this.outputFormat;
		this.outputFormat = outputFormat;
		propertyChangeSupport().firePropertyChange("outputFormat",
				old_outputFormat, outputFormat);
	}

	public void
			setPageBreakAfterEachDocument(boolean pageBreakAfterEachDocument) {
		boolean old_pageBreakAfterEachDocument = this.pageBreakAfterEachDocument;
		this.pageBreakAfterEachDocument = pageBreakAfterEachDocument;
		propertyChangeSupport().firePropertyChange("pageBreakAfterEachDocument",
				old_pageBreakAfterEachDocument, pageBreakAfterEachDocument);
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

	public void setPropertiesSerialized(String propertiesSerialized) {
		this.propertiesSerialized = propertiesSerialized;
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

	public void
			setRepositoryConnection(RepositoryConnection repositoryConnection) {
		this.repositoryConnection = repositoryConnection;
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
		String s = contentDefinition == null ? ""
				: getContentDefinition().toString() + "\n" + "-";
		return s + " Delivery mode: "
				+ CommonUtils.friendlyConstant(getDeliveryMode()) + " - "
				+ " Format: " + CommonUtils.friendlyConstant(getOutputFormat());
	}

	@Override
	public TreeSerializable.Customiser treeSerializationCustomiser() {
		return new Customiser(this);
	}

	public static class Generic<CD extends ContentDefinition>
			extends ContentRequestBase<CD> {
		@Override
		public CD getContentDefinition() {
			return contentDefinition;
		}

		@Override
		public void setContentDefinition(CD contentDefinition) {
			this.contentDefinition = contentDefinition;
		}

		public <S extends Generic<CD>> S
				withContentDefinition(CD contentDefinition) {
			setContentDefinition(contentDefinition);
			return (S) this;
		}
	}
}
