package com.contentmanager.modules.content.specification;

import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.model.ContentType;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * JPA Specifications for dynamic Content search queries.
 * Composes filters without loading the entire table into memory.
 */
public final class ContentSpecification {

    private ContentSpecification() {}

    public static Specification<Content> hasKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("body")), pattern),
                cb.like(cb.lower(root.get("excerpt")), pattern)
        );
    }

    public static Specification<Content> hasStatus(ContentStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Content> hasContentType(ContentType contentType) {
        if (contentType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("contentType"), contentType);
    }

    public static Specification<Content> hasAuthorId(Long authorId) {
        if (authorId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Content> hasCategoryId(Long categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Content> hasTagId(Long tagId) {
        if (tagId == null) return null;
        return (root, query, cb) -> {
            var tagJoin = root.join("tags", JoinType.INNER);
            return cb.equal(tagJoin.get("id"), tagId);
        };
    }

    public static Specification<Content> createdAfter(Instant after) {
        if (after == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    public static Specification<Content> createdBefore(Instant before) {
        if (before == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    public static Specification<Content> publishedAfter(Instant after) {
        if (after == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("publishedAt"), after);
    }

    public static Specification<Content> publishedBefore(Instant before) {
        if (before == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("publishedAt"), before);
    }
}
