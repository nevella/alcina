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

public class DomainTransformException extends Exception implements Serializable {
	private DomainTransformEvent event;

	private String detail;

	private DomainTransformExceptionType type = DomainTransformExceptionType.UNKNOWN;

	private DomainTransformRequest request;

	private String sourceObjectName;

	private boolean silent;

	public DomainTransformException() {
	}

	public DomainTransformException(String message) {
		super(message);
	}

	public DomainTransformException(String message, Throwable cause) {
		super(message, cause);
	}

	public DomainTransformException(Throwable t) {
		super(t);
	}

	public DomainTransformException(DomainTransformEvent event,
			DomainTransformExceptionType type) {
		super(type.toString());
		this.event = event;
		this.type = type;
	}

	public static DomainTransformException wrap(Exception ex,
			DomainTransformEvent event) {
		if (ex instanceof DomainTransformException) {
			return (DomainTransformException) ex;
		}
		DomainTransformException dte = new DomainTransformException(ex);
		dte.setEvent(event);
		return dte;
	}

	public String getDetail() {
		return detail;
	}

	public DomainTransformEvent getEvent() {
		return this.event;
	}

	public DomainTransformRequest getRequest() {
		return this.request;
	}

	public DomainTransformExceptionType getType() {
		return type;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public void setEvent(DomainTransformEvent event) {
		this.event = event;
	}

	public void setRequest(DomainTransformRequest request) {
		this.request = request;
	}

	public void setType(DomainTransformExceptionType type) {
		this.type = type;
	}

	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	public boolean isSilent() {
		return silent;
	}

	public void setSourceObjectName(String sourceObjectName) {
		this.sourceObjectName = sourceObjectName;
	}

	public String getSourceObjectName() {
		return sourceObjectName;
	}

	public enum DomainTransformExceptionType {
		OPTIMISTIC_LOCK_EXCEPTION, SOURCE_ENTITY_NOT_FOUND,
		TARGET_ENTITY_NOT_FOUND, FK_CONSTRAINT_EXCEPTION, VALIDATION_EXCEPTION,
		PERMISSIONS_EXCEPTION, UNKNOWN, TOO_MANY_EXCEPTIONS,
		INVALID_AUTHENTICATION
	}
	public boolean irresolvable(){
		return type==DomainTransformExceptionType.INVALID_AUTHENTICATION||type==DomainTransformExceptionType.TOO_MANY_EXCEPTIONS;
	}
}