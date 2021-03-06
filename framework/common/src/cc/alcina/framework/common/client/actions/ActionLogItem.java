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
package cc.alcina.framework.common.client.actions;

import java.io.Serializable;
import java.util.Date;

import cc.alcina.framework.common.client.util.Ax;

/**
 *
 * @author Nick Reddel
 */
public interface ActionLogItem extends Serializable {
	public Class<? extends RemoteAction> getActionClass();

	public String getActionClassName();

	public Date getActionDate();

	public String getActionLog();

	public String getShortDescription();

	public void setActionClass(Class<? extends RemoteAction> actionClass);

	public void setActionClassName(String actionClassName);

	public void setActionDate(Date actionDate);

	public void setActionLog(String actionLog);

	public void setShortDescription(String shortDescription);

	default boolean provideActionSucceeded() {
		return !Ax.matches(getShortDescription(), "Job failed:.*");
	}
}
