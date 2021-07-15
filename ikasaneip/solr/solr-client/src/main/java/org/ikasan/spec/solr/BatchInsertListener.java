package org.ikasan.spec.solr;

public interface BatchInsertListener<BATCH_EVENT> {

    /**
     * Called when a batch insert occurs
     * @param batchInsertEvent
     */
    public void onBatchInsert(BatchInsertEvent<BATCH_EVENT> batchInsertEvent);
}
