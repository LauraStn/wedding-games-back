package com.weddinggames.backend.game;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.team.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A team's answer to a question. At most one answer per team per question. Editable live by
 * whichever team member currently holds the pen ({@link #controllingParticipant}) right up until
 * the question closes - see the {@code quiz} package.
 */
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "controlling_participant_id")
    private Participant controllingParticipant;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private AnswerModerationStatus moderationStatus = AnswerModerationStatus.PENDING;

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

    public Participant getControllingParticipant() {
        return controllingParticipant;
    }

    public void setControllingParticipant(Participant controllingParticipant) {
        this.controllingParticipant = controllingParticipant;
    }

    public AnswerModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void accept() {
        this.moderationStatus = AnswerModerationStatus.ACCEPTED;
    }

    public void hide() {
        this.moderationStatus = AnswerModerationStatus.HIDDEN;
    }

    /** Resets this team's answer for a fresh attempt: cleared content, no one in control, back to PENDING. */
    public void relaunch() {
        this.content = "";
        this.controllingParticipant = null;
        this.submittedAt = null;
        this.moderationStatus = AnswerModerationStatus.PENDING;
    }
}
