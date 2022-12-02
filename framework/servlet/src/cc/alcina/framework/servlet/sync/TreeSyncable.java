package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.util.HasEquivalenceString;

// instances should only .equals themselves -- and equivalence shd be unique
//
// equivalence is shallow (i.e. exclusive of TreeSyncable children)
public interface TreeSyncable<T> extends HasEquivalenceString<T> {
}
