# sync and merge

sync really involves five phases:  

* __get the models__: get A, C (the two different models)
* __models to interchange format__: A -> B (transform), C -> B' (transform)
* __merge the interchange__: B,B' -> B'' (merge)  
	note - B'' doesn't have to be a superset of union(B,B') - but must be a superset of  intersection (B,B')  
	 (which involves biz logic, particularly wrt "who is the canonical source")  		
* __generate deltas__:B'', B' -> B' delta :: B'', B -> B delta  
	(again, biz logic - some fields may not be stored  in C,A) 
	although, since the final applicators should be aware of this, this stage currently not impl
* __apply deltas__: (C, B' delta) >> C :: (A, B delta) >> A


get the models (domain specific code)
transform to common format (maybe use a 'GraphTransformer')
merge common formats (MergeRequestDispatcher or SyncDispatcher)
generate deltas (done as part of above, SyncMerger)
apply deltas (done as part of above, FlatDeltaPersister)


### SyncDispatchToken:
* domainModel  
	GraphTransformer, PropertyTransformer
* interchangeModel  
	(dispatch) MergeRequestDispatcher
* deltaModel  
	DeltaApplicator


### MergeToken:
* InterchangeModel B
* InterchangeModel B'
* InterchangeMerger: B,B' -> B''
* DeltaOracle: B'', B' > B' delta 
