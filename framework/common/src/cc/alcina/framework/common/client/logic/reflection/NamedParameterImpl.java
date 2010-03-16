package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class NamedParameterImpl implements NamedParameter {
  private boolean booleanValue;
  public boolean booleanValue(){return booleanValue;}
  
  private int intValue;
  public int intValue(){return intValue;}
  
  private Class classValue;
  public Class classValue(){return classValue;}
  
  private String name;
  public String name(){return name;}
  
  private String stringValue;
  public String stringValue(){return stringValue;}
  
  
  public Class<? extends Annotation> annotationType() {
    return NamedParameter.class;
  }
  
  public NamedParameterImpl (boolean booleanValue, int intValue, Class classValue, String name, String stringValue){
    this.booleanValue = booleanValue;
    this.intValue = intValue;
    this.classValue = classValue;
    this.name = name;
    this.stringValue = stringValue;
  }
}
