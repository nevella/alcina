/**
 * <p>
 * This version (v1) of the job system works, but has a high degree of threading
 * complexity. For v1 details, see the main classes - JobRegistry, Scheduler,
 * Context and Allocator
 * 
 * <p>
 * v2 notes
 * <ul>
 * <li>(Clearly) get rid of tx environment abstraction, by bringing mvcc to all
 * (incl client)
 * <li>Have one allocator thread, but multiple allocators. This allows per-child
 * allocators with the same characteristics as 'tl parents', so allocators are
 * truly per-job rather than a hybrid parent/child
 * <li>Don't aim for metadatalock/transform optimisation. All per-job mutations
 * (post-creation) should generally be 1 tx/job
 * <li>All job-metadata-locking operations happen on the allocator queue - not
 * on the jobcontext thread
 * <li>CHILD vs AWAIT relationtype - this depends on what the structure of the
 * parent is - if parent code is interleaved with dependent job performance,
 * AWAIT, if parent just generates a bunch of tasks and then awaits their
 * (sequential or otherwise) completion, CHILD
 * </ul>
 */
package cc.alcina.framework.servlet.job;
