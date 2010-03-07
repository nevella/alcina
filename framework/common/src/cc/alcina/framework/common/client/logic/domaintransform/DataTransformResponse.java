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

package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class DataTransformResponse implements Serializable {
	private long requestId;

	public enum DataTransformResponseResult {
		OK, FAILURE
	}

	private DataTransformResponseResult result = DataTransformResponseResult.OK;

	private List<DataTransformEvent> eventsToUseForClientUpdate = new ArrayList<DataTransformEvent>();

	public DataTransformResponseResult getResult() {
		return this.result;
	}

	public void setResult(DataTransformResponseResult result) {
		this.result = result;
	}

	public List<DataTransformEvent> getEventsToUseForClientUpdate() {
		return eventsToUseForClientUpdate;
	}

	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	public long getRequestId() {
		return requestId;
	}
}
