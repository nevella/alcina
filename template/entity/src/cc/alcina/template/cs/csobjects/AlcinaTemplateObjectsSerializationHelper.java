package cc.alcina.template.cs.csobjects;

import java.util.Date;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.Bookmark;
import cc.alcina.template.cs.persistent.ClassRefImpl;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

@Bean
@Introspectable
/**
 * Old code (predates consort handshake)
 * TODO - abstract
 */
public class AlcinaTemplateObjectsSerializationHelper{ 
//implements 		Entity {
//	public static void preSerialization(ClientInstance clientInstance,
//			LoginState loginState) {
//		AlcinaTemplateObjectsSerializationHelper h = new AlcinaTemplateObjectsSerializationHelper();
//		TransformManager tm = TransformManager.get();
//		tm.registerDomainObject(clientInstance);
//		long localId = tm.nextLocalIdCounter();
//		h.setLocalId(localId);
//		h.setId(1);// avoid create object event;
//		tm.registerDomainObject(h);
//		h.preSerialization0(clientInstance, loginState);
//	}
//
//	private AlcinaTemplateUser currentUser;
//
//	private Date serverDate;
//
//	private ClientInstance clientInstance;
//
//	private long id;
//
//	private long localId;
//
//	private LoginState loginState;
//
//	public ClientInstance getClientInstance() {
//		return this.clientInstance;
//	}
//
//	public AlcinaTemplateUser getCurrentUser() {
//		return this.currentUser;
//	}
//
//	public long getId() {
//		return this.id;
//	}
//
//	public long getLocalId() {
//		return this.localId;
//	}
//
//	public LoginState getLoginState() {
//		return loginState;
//	}
//
//	public Date getServerDate() {
//		return this.serverDate;
//	}
//
//	@SuppressWarnings("unchecked")
//	public AlcinaTemplateObjects postDeserialization() {
//		TransformManager tm = TransformManager.get();
//		AlcinaTemplateObjects objects = new AlcinaTemplateObjects();
//		objects.setCurrentUser(currentUser);
//		objects.setServerDate(serverDate);
//		objects.setClassRefs((Set) tm
//				.registeredObjectsAsSet(ClassRefImpl.class));
//		objects.setGeneralProperties(tm
//				.registeredSingleton(GeneralProperties.class));
//		objects.setGroups(tm.registeredObjectsAsSet(AlcinaTemplateGroup.class));
//		objects.setBookmarks(tm.registeredObjectsAsSet(Bookmark.class));
//		return objects;
//	}
//
//	public void setClientInstance(ClientInstance clientInstance) {
//		this.clientInstance = clientInstance;
//	}
//
//	public void setCurrentUser(AlcinaTemplateUser currentUser) {
//		this.currentUser = currentUser;
//	}
//
//	public void setId(long id) {
//		this.id = id;
//	}
//
//	public void setLocalId(long localId) {
//		this.localId = localId;
//	}
//
//	public void setLoginState(LoginState loginState) {
//		this.loginState = loginState;
//	}
//
//	public void setServerDate(Date serverDate) {
//		this.serverDate = serverDate;
//	}
//
//	private void preSerialization0(ClientInstance clientInstance,
//			LoginState loginState) {
//		AlcinaTemplateObjects co = AlcinaTemplateObjects.current();
//		currentUser = co.getCurrentUser();
//		serverDate = co.getServerDate();
//		this.clientInstance = clientInstance;
//		this.loginState = loginState;
//		id = 1;
//	}
}