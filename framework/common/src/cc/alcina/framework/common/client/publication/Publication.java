package cc.alcina.framework.common.client.publication;

import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.customiser.FriendlyEnumCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.ObjectActionLinkCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.StandardLabelCustomiser;

@ObjectPermissions(create = @Permission(access = AccessLevel.EVERYONE), delete = @Permission(access = AccessLevel.ADMIN_OR_OWNER))
@MappedSuperclass
@Bean
@DomainTransformPropagation(value = PropagationType.NON_PERSISTENT, persistNonRoot = false)
@Registration({ PersistentImpl.class, Publication.class })
public abstract class Publication extends Entity<Publication>
		implements HasOwner, HasIUser, SearchResult {
	public static final transient String REPUBLISH_ACTION_NAME = "View";

	private Definition definition;

	private String publicationType;

	private Date publicationDate;

	private Long userPublicationId;

	@GwtTransient
	private String publicationUid;

	@GwtTransient
	private String mimeMessageId;

	@GwtTransient
	private String definitionSignature;

	private String definitionSerialized;

	private String definitionDescription;

	private String definitionClassName;

	private String serializedPublication;

	@Transient
	@Display(name = "Actions", orderingHint = 12, visible = @Permission(access = AccessLevel.LOGGED_IN))
	@Custom(customiserClass = ObjectActionLinkCustomiser.class, parameters = {
			@NamedParameter(name = ObjectActionLinkCustomiser.ACTION_CLASS, classValue = PublicationRepublishLink.class) })
	public String getActions() {
		return "actions";
	}

	@Transient
	@DomainProperty(serialize = true)
	public Definition getDefinition() {
		definition = TransformManager.resolveMaybeDeserialize(definition,
				this.definitionSerialized, null,
				Reflections.forName(definitionClassName));
		return this.definition;
	}

	public String getDefinitionClassName() {
		return this.definitionClassName;
	}

	@Lob
	@Transient
	@Display(name = "Content", orderingHint = 50, visible = @Permission(access = AccessLevel.LOGGED_IN))
	@Custom(customiserClass = StandardLabelCustomiser.class)
	public String getDefinitionDescription() {
		return this.definitionDescription;
	}

	@Lob
	@Transient
	public String getDefinitionSerialized() {
		return this.definitionSerialized;
	}

	public String getDefinitionSignature() {
		return this.definitionSignature;
	}

	public String getMimeMessageId() {
		return this.mimeMessageId;
	}

	@Transient
	public abstract Publication getOriginalPublication();

	@Override
	@Transient
	@XmlTransient
	public IUser getOwner() {
		return getUser();
	}

	@Display(name = "Date", orderingHint = 20, visible = @Permission(access = AccessLevel.LOGGED_IN))
	public Date getPublicationDate() {
		return publicationDate;
	}

	@Display(name = "Type", orderingHint = 30, visible = @Permission(access = AccessLevel.LOGGED_IN))
	@Custom(customiserClass = FriendlyEnumCustomiser.class)
	public String getPublicationType() {
		return this.publicationType;
	}

	public String getPublicationUid() {
		return this.publicationUid;
	}

	@Transient
	@Lob
	public String getSerializedPublication() {
		return serializedPublication;
	}

	@Override
	@Transient
	public abstract IUser getUser();

	@Display(name = "Publication id", orderingHint = 10, visible = @Permission(access = AccessLevel.LOGGED_IN))
	public Long getUserPublicationId() {
		return this.userPublicationId;
	}

	public ContentDefinition provideContentDefinition() {
		return getDefinition().provideContentDefinition();
	}

	public DeliveryModel provideDeliveryModel() {
		return getDefinition().provideDeliveryModel();
	}

	public void setDefinition(Definition definition) {
		Definition old_definition = this.definition;
		this.definition = definition;
		propertyChangeSupport().firePropertyChange("definition", old_definition,
				definition);
	}

	public void setDefinitionClassName(String definitionClassName) {
		String old_definitionClassName = this.definitionClassName;
		this.definitionClassName = definitionClassName;
		propertyChangeSupport().firePropertyChange("definitionClassName",
				old_definitionClassName, definitionClassName);
	}

	public void setDefinitionDescription(String definitionDescription) {
		String old_definitionDescription = this.definitionDescription;
		this.definitionDescription = definitionDescription;
		propertyChangeSupport().firePropertyChange("definitionDescription",
				old_definitionDescription, definitionDescription);
	}

	public void setDefinitionSerialized(String definitionSerialized) {
		String old_definitionSerialized = this.definitionSerialized;
		this.definitionSerialized = definitionSerialized;
		propertyChangeSupport().firePropertyChange("definitionSerialized",
				old_definitionSerialized, definitionSerialized);
	}

	public void setDefinitionSignature(String definitionSignature) {
		String old_definitionSignature = this.definitionSignature;
		this.definitionSignature = definitionSignature;
		propertyChangeSupport().firePropertyChange("definitionSignature",
				old_definitionSignature, definitionSignature);
	}

	@Override
	public void setId(long id) {
		super.setId(id);
	}

	public void setMimeMessageId(String mimeMessageId) {
		String old_mimeMessageId = this.mimeMessageId;
		this.mimeMessageId = mimeMessageId;
		propertyChangeSupport().firePropertyChange("mimeMessageId",
				old_mimeMessageId, mimeMessageId);
	}

	public abstract void
			setOriginalPublication(Publication originalPublication);

	public void setPublicationDate(Date publicationDate) {
		Date old_publicationDate = this.publicationDate;
		this.publicationDate = publicationDate;
		propertyChangeSupport().firePropertyChange("publicationDate",
				old_publicationDate, publicationDate);
	}

	public void setPublicationType(String publicationType) {
		String old_publicationType = this.publicationType;
		this.publicationType = publicationType;
		propertyChangeSupport().firePropertyChange("publicationType",
				old_publicationType, publicationType);
	}

	public void setPublicationUid(String publicationUid) {
		String old_publicationUid = this.publicationUid;
		this.publicationUid = publicationUid;
		propertyChangeSupport().firePropertyChange("publicationUid",
				old_publicationUid, publicationUid);
	}

	public void setSerializedPublication(String serializedPublication) {
		String old_serializedPublication = this.serializedPublication;
		this.serializedPublication = serializedPublication;
		propertyChangeSupport().firePropertyChange("serializedPublication",
				old_serializedPublication, serializedPublication);
	}

	@Override
	public abstract void setUser(IUser user);

	public void setUserPublicationId(Long userPublicationId) {
		this.userPublicationId = userPublicationId;
	}

	public interface Definition {
		public ContentDefinition provideContentDefinition();

		public DeliveryModel provideDeliveryModel();
	}

	@Reflected
	public static class PublicationRepublishLink extends PermissibleAction {
		@Override
		@AlcinaTransient
		public String getDisplayName() {
			return "Republish";
		}
	}
}
