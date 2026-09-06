package com.weddinggames.backend.whosaidit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.who-said-it")
public class WhoSaidItProperties {

    /** Max characters allowed in a proposed question's content. */
    private int maxContentLength = 280;

    /** Max number of questions a single participant may have proposed at once. */
    private int maxQuestionsPerParticipant = 2;

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public int getMaxQuestionsPerParticipant() {
        return maxQuestionsPerParticipant;
    }

    public void setMaxQuestionsPerParticipant(int maxQuestionsPerParticipant) {
        this.maxQuestionsPerParticipant = maxQuestionsPerParticipant;
    }
}
