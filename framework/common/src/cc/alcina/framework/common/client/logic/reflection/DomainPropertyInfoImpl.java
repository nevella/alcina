package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;

@SuppressWarnings("all")
public class DomainPropertyInfoImpl implements DomainPropertyInfo {
  private boolean cloneForDuplication;
  public boolean cloneForDuplication(){return cloneForDuplication;}
  
  private boolean cloneForProvisionalEditing;
  public boolean cloneForProvisionalEditing(){return cloneForProvisionalEditing;}
  
  private boolean eagerCreation;
  public boolean eagerCreation(){return eagerCreation;}
  
  private boolean registerChildren;
  public boolean registerChildren(){return registerChildren;}
  
  private boolean serializeOnClient;
  public boolean serializeOnClient(){return serializeOnClient;}
  
  private boolean silentFailOnIllegalWrites;
  public boolean silentFailOnIllegalWrites(){return silentFailOnIllegalWrites;}
  
  private boolean stripTags;
  public boolean stripTags(){return stripTags;}
  
  
  public Class<? extends Annotation> annotationType() {
    return DomainPropertyInfo.class;
  }
  
  public DomainPropertyInfoImpl (boolean cloneForDuplication, boolean cloneForProvisionalEditing, boolean eagerCreation, boolean registerChildren, boolean serializeOnClient, boolean silentFailOnIllegalWrites, boolean stripTags){
    this.cloneForDuplication = cloneForDuplication;
    this.cloneForProvisionalEditing = cloneForProvisionalEditing;
    this.eagerCreation = eagerCreation;
    this.registerChildren = registerChildren;
    this.serializeOnClient = serializeOnClient;
    this.silentFailOnIllegalWrites = silentFailOnIllegalWrites;
    this.stripTags = stripTags;
  }
}
