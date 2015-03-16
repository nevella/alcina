# Introduction #

These occupy some of the same functional space as the GWT UIBinder tools - I haven't used the UIBinder at all (because these exist) - I'd be interested to see if the two could be effectively combined.


# Details #

**Annotated property example**
```
        @ManyToMany(mappedBy = "memberUsers", targetEntity = AlcinaTemplateGroup.class)
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Groups", orderingHint = 96))
	@Association(implementationClass = AlcinaTemplateGroup.class, propertyName = "memberUsers")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER))
	@CustomiserInfo(customiserClass = SelectorCustomiser.class)
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	@XmlTransient
	public Set<? extends IGroup> getSecondaryGroups() {
		return (Set<AlcinaTemplateGroup>) this.secondaryGroups;
	}
```

**Annotated form creation example**
```
    ContentViewFactory cvf = new ContentViewFactory();
    pcm = new ChangePasswordModel();
    PermissibleActionEvent.PermissibleActionListener vl = new PermissibleActionEvent.PermissibleActionListener() {
    ...
    Widget changePasswordView = cvf.createBeanView(pcm, true, vl, false, true);
```

**Javadoc**
[cc.alcina.framework.common.client.logic.reflection.Association](http://alcina.googlecode.com/svn/trunk/javadoc/trunk/cc/alcina/framework/common/client/logic/reflection/Association.html)