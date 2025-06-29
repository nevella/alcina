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
/*
 * FIXME - categorise:
 * @formatter:off
 
Client request persistence uniqueness - this needs to be enforced during request table creation (if clustered backends) via:
exSql("create index domaintransformrequest_chunk_uuid_idx on domaintransformrequest using btree( chunk_uuid);",
		true);
exSql("ALTER TABLE domaintransformrequest ADD CONSTRAINT domaintransformrequest_chunk_uuid_unique unique (chunk_uuid);",
		true);


		
 * @formatter:on
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_Mvcc extends Feature {
}
