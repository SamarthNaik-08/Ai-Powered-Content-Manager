package com.contentmanager.modules.content.service;

import com.contentmanager.modules.content.model.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ContentWorkflowService Tests")
class ContentWorkflowServiceTest {

    private final ContentWorkflowService workflowService = new ContentWorkflowService();

    // ========================= VALID TRANSITIONS =========================

    @Test
    @DisplayName("DRAFT -> IN_REVIEW is allowed")
    void draftToInReviewAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.DRAFT, ContentStatus.IN_REVIEW)).isTrue();
    }

    @Test
    @DisplayName("DRAFT -> ARCHIVED is allowed")
    void draftToArchivedAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.DRAFT, ContentStatus.ARCHIVED)).isTrue();
    }

    @Test
    @DisplayName("IN_REVIEW -> APPROVED is allowed")
    void inReviewToApprovedAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.IN_REVIEW, ContentStatus.APPROVED)).isTrue();
    }

    @Test
    @DisplayName("IN_REVIEW -> REJECTED is allowed")
    void inReviewToRejectedAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.IN_REVIEW, ContentStatus.REJECTED)).isTrue();
    }

    @Test
    @DisplayName("APPROVED -> PUBLISHED is allowed")
    void approvedToPublishedAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.APPROVED, ContentStatus.PUBLISHED)).isTrue();
    }

    @Test
    @DisplayName("APPROVED -> SCHEDULED is allowed")
    void approvedToScheduledAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.APPROVED, ContentStatus.SCHEDULED)).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED -> PUBLISHED is allowed")
    void scheduledToPublishedAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.SCHEDULED, ContentStatus.PUBLISHED)).isTrue();
    }

    @Test
    @DisplayName("PUBLISHED -> ARCHIVED is allowed")
    void publishedToArchivedAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.PUBLISHED, ContentStatus.ARCHIVED)).isTrue();
    }

    @Test
    @DisplayName("REJECTED -> DRAFT is allowed (resubmission)")
    void rejectedToDraftAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.REJECTED, ContentStatus.DRAFT)).isTrue();
    }

    @Test
    @DisplayName("ARCHIVED -> DRAFT is allowed (unarchive)")
    void archivedToDraftAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.ARCHIVED, ContentStatus.DRAFT)).isTrue();
    }

    // ========================= INVALID TRANSITIONS =========================

    @Test
    @DisplayName("DRAFT -> PUBLISHED is NOT allowed (must go through review)")
    void draftToPublishedNotAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.DRAFT, ContentStatus.PUBLISHED)).isFalse();
    }

    @Test
    @DisplayName("DRAFT -> APPROVED is NOT allowed (must go through review)")
    void draftToApprovedNotAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.DRAFT, ContentStatus.APPROVED)).isFalse();
    }

    @Test
    @DisplayName("IN_REVIEW -> PUBLISHED is NOT allowed (must approve first)")
    void inReviewToPublishedNotAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.IN_REVIEW, ContentStatus.PUBLISHED)).isFalse();
    }

    @Test
    @DisplayName("PUBLISHED -> DRAFT is NOT allowed")
    void publishedToDraftNotAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.PUBLISHED, ContentStatus.DRAFT)).isFalse();
    }

    @Test
    @DisplayName("REJECTED -> PUBLISHED is NOT allowed")
    void rejectedToPublishedNotAllowed() {
        assertThat(workflowService.isTransitionAllowed(ContentStatus.REJECTED, ContentStatus.PUBLISHED)).isFalse();
    }

    // ========================= VALIDATE TRANSITION (throws) =========================

    @Test
    @DisplayName("validateTransition throws for invalid transition")
    void validateTransitionThrowsForInvalid() {
        assertThatThrownBy(() -> workflowService.validateTransition(ContentStatus.DRAFT, ContentStatus.PUBLISHED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from DRAFT to PUBLISHED");
    }

    @Test
    @DisplayName("validateTransition does not throw for valid transition")
    void validateTransitionDoesNotThrowForValid() {
        assertThatCode(() -> workflowService.validateTransition(ContentStatus.DRAFT, ContentStatus.IN_REVIEW))
                .doesNotThrowAnyException();
    }
}
