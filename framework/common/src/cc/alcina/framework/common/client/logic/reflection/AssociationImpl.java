package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class AssociationImpl implements Association {
  private boolean deletionAllowed;
  public boolean deletionAllowed(){return deletionAllowed;}
  
  private boolean silentUpdates;
  public boolean silentUpdates(){return silentUpdates;}
  
  private Class implementationClass;
  public Class implementationClass(){return implementationClass;}
  
  private String propertyName;
  public String propertyName(){return propertyName;}
  
  
  public Class<? extends Annotation> annotationType() {
    return Association.class;
  }
  
  public AssociationImpl (boolean deletionAllowed, boolean silentUpdates, Class implementationClass, String propertyName){
    this.deletionAllowed = deletionAllowed;
    this.silentUpdates = silentUpdates;
    this.implementationClass = implementationClass;
    this.propertyName = propertyName;
  }
}
