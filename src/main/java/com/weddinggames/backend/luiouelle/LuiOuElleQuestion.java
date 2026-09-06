package com.weddinggames.backend.luiouelle;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A "Lui ou Elle" question proposed by a guest about the couple, while the lobby is still open. */
@Entity
@Table(name = "lui_ou_elle_question")
public class LuiOuElleQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Participant author;

    @Column(nullable = false, length = 500)
    private String content;

    protected LuiOuElleQuestion() {}

    public LuiOuElleQuestion(WeddingEvent event, Participant author, String content) {
        this.event = event;
        this.author = author;
        this.content = content;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public Participant getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
