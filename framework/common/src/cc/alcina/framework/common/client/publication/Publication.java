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

import java.util.Date;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.permissions.IUser;

/**
 *
 * @author Nick Reddel
 */
public interface Publication extends HasId {
	public ContentDefinition getContentDefinition();

	public String getContentDefinitionDescription();

	public Long getContentDefinitionWrapperId();

	public DeliveryModel getDeliveryModel();

	public String getDeliveryModelDescription();

	public Long getDeliveryModelWrapperId();

	public Publication getOriginalPublication();

	public Date getPublicationDate();

	public String getPublicationType();

	public IUser getUser();

	public Long getUserPublicationId();

	public void setContentDefinition(ContentDefinition contentDefinition);

	public void setContentDefinitionDescription(
			String contentDefinitionDescription);

	public void setContentDefinitionWrapperId(Long contentDefinitionWrapperId);

	public void setDeliveryModel(DeliveryModel deliveryModel);

	public void setDeliveryModelDescription(String deliveryModelDescription);

	public void setDeliveryModelWrapperId(Long deliveryModelWrapperId);

	public void setOriginalPublication(Publication originalPublication);

	public void setPublicationDate(Date publicationDate);

	public void setPublicationType(String publicationType);

	public void setUser(IUser user);

	public void setUserPublicationId(Long userPublicationId);

	String getPublicationUid();

	default void setMimeMessageId(String messageId) {
	}

	void setPublicationUid(String publicationUid);
	
	String getSerializedPublication();
	void setSerializedPublication(String serializedPublication);
}
