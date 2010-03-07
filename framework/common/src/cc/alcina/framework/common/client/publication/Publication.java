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

import cc.alcina.framework.common.client.logic.permissions.HasId;
import cc.alcina.framework.common.client.logic.permissions.IUser;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public interface Publication extends HasId{
	
	public Long getUserPublicationId();
	public void setUserPublicationId(Long userPublicationId);
	public String getContentDefinitionDescription();
	public void setContentDefinitionDescription(String contentDefinitionDescription);
	public String getDeliveryModelDescription();
	public void setDeliveryModelDescription(String deliveryModelDescription);
	public ContentDefinition getContentDefinition();
	public Long getContentDefinitionWrapperId();
	public DeliveryModel getDeliveryModel();
	public Long getDeliveryModelWrapperId();
	public Publication getOriginalPublication();
	public String getPublicationType();
	public Date getPublicationDate();
	public void setContentDefinition(ContentDefinition contentDefinition);
	public void setContentDefinitionWrapperId(Long contentDefinitionWrapperId);
	public void setDeliveryModel(DeliveryModel deliveryModel);
	public void setDeliveryModelWrapperId(Long deliveryModelWrapperId);
	public void setOriginalPublication(Publication originalPublication);
	public void setPublicationDate(Date publicationDate);
	public void setUser(IUser user);
	public void setPublicationType(String publicationType);
	public IUser getUser();
	
}
