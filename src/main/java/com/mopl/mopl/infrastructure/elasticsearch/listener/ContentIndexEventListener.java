package com.mopl.mopl.infrastructure.elasticsearch.listener;

import com.mopl.mopl.infrastructure.elasticsearch.ContentIndexService;
import com.mopl.mopl.infrastructure.elasticsearch.event.ContentIndexEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class ContentIndexEventListener
{
    private final ContentIndexService contentIndexService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContentIndexEvent event) {
        contentIndexService.index(event.content());
    }
}
