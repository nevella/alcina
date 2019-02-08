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
 * @author Nick Reddel
 */
public class DomainTransformResponse implements Serializable {
    private long requestId;

    private int transformsProcessed;

    private String message;

    private DomainTransformResponseResult result = DomainTransformResponseResult.OK;

    private List<DomainTransformEvent> eventsToUseForClientUpdate = new ArrayList<DomainTransformEvent>();

    private List<DomainTransformException> transformExceptions = new ArrayList<DomainTransformException>();

    private String logOffset;

    // only when error
    private DomainTransformRequest request;

    public List<DomainTransformEvent> getEventsToUseForClientUpdate() {
        return eventsToUseForClientUpdate;
    }

    public String getLogOffset() {
        return this.logOffset;
    }

    public String getMessage() {
        return this.message;
    }

    public DomainTransformRequest getRequest() {
        return this.request;
    }

    public long getRequestId() {
        return requestId;
    }

    public DomainTransformResponseResult getResult() {
        return this.result;
    }

    public List<DomainTransformException> getTransformExceptions() {
        return this.transformExceptions;
    }

    public int getTransformsProcessed() {
        return this.transformsProcessed;
    }

    public void setLogOffset(String logOffset) {
        this.logOffset = logOffset;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRequest(DomainTransformRequest request) {
        this.request = request;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public void setResult(DomainTransformResponseResult result) {
        this.result = result;
    }

    public void setTransformsProcessed(int transformsProcessed) {
        this.transformsProcessed = transformsProcessed;
    }

    public String toExceptionString() {
        StringBuffer sb = new StringBuffer();
        for (DomainTransformException ex : getTransformExceptions()) {
            sb.append(ex);
            sb.append("\n");
        }
        return sb.toString();
    }

    public enum DomainTransformResponseResult {
        OK, FAILURE
    }
}
