package com.mopl.mopl.infrastructure.batch.exception;

public class TmdbBatchCollectException extends BatchException
{
    public TmdbBatchCollectException() {
        super(BatchErrorCode.BATCH_TMDB_COLLECT_FAILED);
    }
}
