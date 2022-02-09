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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.Publication.Definition;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.logic.reflection.Registration;

/**
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

    public List<MultipleDeliveryEntry> getMultipleDeliveryEntries();

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

    // Parameters for ContentDeliveryType_MULTIPLE
    @RegistryLocation(registryPoint = TreeSerializable.class)
    @Registration(TreeSerializable.class)
    public static class MultipleDeliveryEntry implements TreeSerializable {

        private String emailSubject;

        private String emailAddresses;

        private String fileName;

        private String transformerClassName;

        private String deliveryMode = ContentDeliveryType.DOWNLOAD.serializedForm();

        private String transformerPropertiesSerialized;

        public void addTransformerProperty(String key, String value) {
            StringMap map = provideTransformerProperties();
            map.put(key, value);
            setTransformerPropertiesSerialized(map.toPropertyString());
        }

        public String getDeliveryMode() {
            return this.deliveryMode;
        }

        public String getEmailAddresses() {
            return this.emailAddresses;
        }

        public String getEmailSubject() {
            return this.emailSubject;
        }

        public String getFileName() {
            return this.fileName;
        }

        public String getTransformerClassName() {
            return this.transformerClassName;
        }

        public String getTransformerPropertiesSerialized() {
            return this.transformerPropertiesSerialized;
        }

        public ContentDeliveryType provideContentDeliveryType() {
            return ExtensibleEnum.valueOf(ContentDeliveryType.class, deliveryMode);
        }

        public StringMap provideTransformerProperties() {
            return StringMap.fromPropertyString(getTransformerPropertiesSerialized());
        }

        public void putContentDeliveryType(ContentDeliveryType type) {
            setDeliveryMode(type == null ? null : type.name());
        }

        public void setDeliveryMode(String deliveryMode) {
            this.deliveryMode = deliveryMode;
        }

        public void setEmailAddresses(String emailAddresses) {
            this.emailAddresses = emailAddresses;
        }

        public void setEmailSubject(String emailSubkect) {
            this.emailSubject = emailSubkect;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public void setTransformerClassName(String transformerClassName) {
            this.transformerClassName = transformerClassName;
        }

        public void setTransformerPropertiesSerialized(String transformerPropertiesSerialized) {
            this.transformerPropertiesSerialized = transformerPropertiesSerialized;
        }

        public interface Transformer extends BiFunction<InputStream, StringMap, InputStream> {
        }
    }
}
