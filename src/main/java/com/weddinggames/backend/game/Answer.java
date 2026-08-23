package com.weddinggames.backend.game;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.team.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A team's answer to a question. At most one answer per team per question. */
@Entity
@Table(name = "answer")
public class Answer extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    protected Answer() {}

    public Answer(Question question, Team team, String content, Instant submittedAt) {
        this.question = question;
        this.team = team;
        this.content = content;
        this.submittedAt = submittedAt;
    }

    public Question getQuestion() {
        return question;
    }

    public Team getTeam() {
        return team;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}
