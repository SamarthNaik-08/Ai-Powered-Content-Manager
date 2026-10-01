package com.contentmanager.modules.content.service;

import com.contentmanager.modules.content.model.ContentStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Enforces strict content workflow state transitions.
 * No arbitrary status changes are allowed.
 */
@Service
public class ContentWorkflowService {

    /**
     * Defines the allowed state transitions.
     * Key = current status, Value = set of allowed target statuses.
     */
    private static final Map<ContentStatus, Set<ContentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ContentStatus.DRAFT, Set.of(ContentStatus.IN_REVIEW, ContentStatus.ARCHIVED),
            ContentStatus.IN_REVIEW, Set.of(ContentStatus.APPROVED, ContentStatus.REJECTED),
            ContentStatus.APPROVED, Set.of(ContentStatus.PUBLISHED, ContentStatus.SCHEDULED, ContentStatus.ARCHIVED),
            ContentStatus.SCHEDULED, Set.of(ContentStatus.PUBLISHED, ContentStatus.ARCHIVED),
            ContentStatus.PUBLISHED, Set.of(ContentStatus.ARCHIVED),
            ContentStatus.REJECTED, Set.of(ContentStatus.DRAFT, ContentStatus.ARCHIVED),
            ContentStatus.ARCHIVED, Set.of(ContentStatus.DRAFT)
    );

    /**
     * Check if a transition from currentStatus to targetStatus is valid.
     */
    public boolean isTransitionAllowed(ContentStatus currentStatus, ContentStatus targetStatus) {
        Set<ContentStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * Get a human-readable message about what transitions are valid from the given status.
     */
    public String getAllowedTransitionsMessage(ContentStatus currentStatus) {
        Set<ContentStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || allowed.isEmpty()) {
            return "No transitions allowed from status: " + currentStatus;
        }
        return "Allowed transitions from " + currentStatus + ": " + allowed;
    }

    /**
     * Validate and return the target status. Throws if invalid.
     */
    public void validateTransition(ContentStatus currentStatus, ContentStatus targetStatus) {
        if (!isTransitionAllowed(currentStatus, targetStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + currentStatus + " to " + targetStatus + ". " +
                            getAllowedTransitionsMessage(currentStatus));
        }
    }
}
