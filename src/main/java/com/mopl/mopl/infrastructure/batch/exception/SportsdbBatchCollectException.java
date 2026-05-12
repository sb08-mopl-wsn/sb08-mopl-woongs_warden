package com.mopl.mopl.infrastructure.batch.exception;

public class SportsdbBatchCollectException extends BatchException
{
    public SportsdbBatchCollectException() {
        super(BatchErrorCode.BATCH_SPORTSDB_COLLECT_FAILED);
    }
}
