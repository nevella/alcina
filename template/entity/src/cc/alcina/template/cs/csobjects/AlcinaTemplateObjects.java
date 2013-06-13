package cc.alcina.template.cs.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolderProvider;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.Bookmark;


public class AlcinaTemplateObjects implements Serializable, DomainModelHolder,
		CollectionModificationListener,
		DomainModelHolderProvider<AlcinaTemplateObjects> {
	private Set<AlcinaTemplateGroup> groups = new LinkedHashSet<AlcinaTemplateGroup>();

	private String onetimeMessage;

	private AlcinaTemplateUser currentUser;

	private Date serverDate;

	private String homepageHtml;

	private GeneralProperties generalProperties;

	private Set<ClassRef> classRefs = new LinkedHashSet<ClassRef>();

	private Set<Bookmark> bookmarks = new LinkedHashSet<Bookmark>();
	
	private static DomainModelHolderProvider<AlcinaTemplateObjects> provider;

	public static AlcinaTemplateObjects current() {
		if (provider != null) {
			return provider.getDomainModelHolder();
		}
		throw new WrappedRuntimeException("No IJO provider registered",
				SuggestedAction.NOTIFY_ERROR);
	}

	public static DomainModelHolderProvider<AlcinaTemplateObjects> getProvider() {
		return provider;
	}

	public static void registerProvider(
			DomainModelHolderProvider<AlcinaTemplateObjects> p) {
		provider = p;
	}

	public AlcinaTemplateObjects() {
	}

	public void collectionModification(CollectionModificationEvent evt) {
		if (evt.getSource().equals(TransformManager.get())
				&& evt.getCollectionClass() != null) {
		}
	}

	public Set<ClassRef> getClassRefs() {
		return classRefs;
	}

	public AlcinaTemplateUser getCurrentUser() {
		return this.currentUser;
	}

	public AlcinaTemplateObjects getDomainModelHolder() {
		return this;
	}

	public GeneralProperties getGeneralProperties() {
		return this.generalProperties;
	}

	public Set<AlcinaTemplateGroup> getGroups() {
		return this.groups;
	}

	public String getHomepageHtml() {
		return this.homepageHtml;
	}

	public String getOnetimeMessage() {
		return onetimeMessage;
	}

	public Date getServerDate() {
		return this.serverDate;
	}

	@SuppressWarnings("unchecked")
	public List registerableDomainObjects() {
		Object[] colls = new Object[] { generalProperties, groups, currentUser,
				classRefs, bookmarks };
		return new ArrayList(Arrays.asList(colls));
	}

	public void registerListeners() {
		TransformManager.get().addCollectionModificationListener(this);
	}

	public void registerSelfAsProvider() {
		registerProvider(this);
	}

	public void removeListeners() {
		TransformManager.get().removeCollectionModificationListener(this);
	}

	public void setClassRefs(Set<ClassRef> classRefs) {
		this.classRefs = classRefs;
	}

	public void setCurrentUser(AlcinaTemplateUser currentUser) {
		this.currentUser = currentUser;
	}

	public void setGeneralProperties(GeneralProperties generalProperties) {
		this.generalProperties = generalProperties;
	}

	public void setGroups(Set<AlcinaTemplateGroup> groups) {
		this.groups = groups;
	}

	public void setHomepageHtml(String homepageHtml) {
		this.homepageHtml = homepageHtml;
	}

	public void setOnetimeMessage(String onetimeMessage) {
		this.onetimeMessage = onetimeMessage;
	}

	public void setServerDate(Date serverDate) {
		this.serverDate = serverDate;
	}

	public void setBookmarks(Set<Bookmark> bookmarks) {
		this.bookmarks = bookmarks;
	}

	public Set<Bookmark> getBookmarks() {
		return bookmarks;
	}
}
