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
package cc.alcina.framework.common.client.csobjects;

/**
 * 
 * @author Nick Reddel
 */
public enum LogMessageType {
	INVALID_AUTHENTICATION, TRANSFORM_EXCEPTION, PUBLICATION_EXCEPTION, INFO,
	CLIENT_EXCEPTION, RPC_EXCEPTION, OFFLINE_TRANSFORM_MERGE_EXCEPTION,
	TRANSFORM_CONFLICT, PERMISSIONS_EXCEPTION, LOCAL_DOM_EXCEPTION
}
