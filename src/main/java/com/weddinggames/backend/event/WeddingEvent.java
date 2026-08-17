package com.weddinggames.backend.event;

import com.weddinggames.backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "wedding_event")
public class WeddingEvent extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10)
    private String language = "fr-FR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visual_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> visualConfig = new HashMap<>();

    protected WeddingEvent() {}

    public WeddingEvent(String slug, String title, String language) {
        this.slug = slug;
        this.title = title;
        this.language = language;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Map<String, Object> getVisualConfig() {
        return visualConfig;
    }

    public void setVisualConfig(Map<String, Object> visualConfig) {
        this.visualConfig = visualConfig;
    }
}
