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


/**
 *
 * @author Nick Reddel
 */

 public interface DeliveryModel {
	public boolean isCoverPage();
	public boolean isFooter();
	public boolean isPageBreakAfterEachDocument();
	public FormatConversionTarget provideTargetFormat();
	public ContentDeliveryType provideContentDeliveryType();
	public String getEmailAddress();
	public String getSystemEmailAddressOfRequestor();
	public String getSuggestedFileName();
	public String getEmailSubject();
	public String getEmailSubjectForRequestor();
	public boolean isEmailInline();
	public String getAttachmentMessage() ;
	public String getAttachmentMessageForRequestor() ;
	public boolean isNoPersistence();
	public boolean isTest();
}
