package com.mopl.mopl.domain.review.event;

import com.mopl.mopl.infrastructure.ai.service.UserTasteProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReviewEventListener
{
    private final UserTasteProfileService userTasteProfileService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewChanged(ReviewChangedEvent event) {
        userTasteProfileService.evictTasteProfile(event.userId());
    }
}
