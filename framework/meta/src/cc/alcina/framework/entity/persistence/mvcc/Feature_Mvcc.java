package cc.alcina.framework.entity.persistence.mvcc;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * <p>
 * This component implements transactions and transactional objects for the
 * Domain component
 * 
 * <pre>
 * <code>
 
The mvcc component is complete - albeit with several FIXMEs. The current priority is implementing documentation, 
other improvements are tracked in the FIXMEs and ALC- tickets

 # Mvcc documentation tracking

 - Diagrams
  - uml/entity - [Domain, Transactions, Transaction, TLTM, Entity, ObjectVersions, ObjectVersion]
  - uml/flow - Transaction/Tltm (observables emitted by transforms as they commit)
  - uml/flow - Entity/ObjectVersions/ObjectVersion
  - uml/flow - Tltm/indexing

  
- Package doc
 - v2 - overview referencing the package diagrams, with a description of the principal actors
 - v2 - example - YACCA - yet anther croissanteria consumption app

 * </code>
 * </pre>
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_Mvcc extends Feature {
}
