package com.weddinggames.backend.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.team.dto.MyTeamResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the participant-facing character reveal. */
class TeamParticipantServiceTest {

    private TeamMemberRepository teamMemberRepository;
    private TeamParticipantService service;

    @BeforeEach
    void setUp() {
        teamMemberRepository = mock(TeamMemberRepository.class);
        service = new TeamParticipantService(teamMemberRepository);
    }

    private Participant mockParticipant(UUID id, String displayName) {
        Participant participant = mock(Participant.class);
        when(participant.getId()).thenReturn(id);
        when(participant.getDisplayName()).thenReturn(displayName);
        return participant;
    }

    private GameCharacter mockCharacter(String name) {
        GameCharacter character = mock(GameCharacter.class);
        when(character.getId()).thenReturn(UUID.randomUUID());
        when(character.getName()).thenReturn(name);
        when(character.getAvatarUrl()).thenReturn(null);
        when(character.getDescription()).thenReturn(null);
        return character;
    }

    @Test
    void returnsMyCharacterAndMyPartnersCharacterForABinome() {
        UUID meId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        Team team = new Team(event);

        GameCharacter myCharacter = mockCharacter("Sangoku");
        TeamMember myMembership = new TeamMember(team, mockParticipant(meId, "Me"));
        myMembership.setCharacter(myCharacter);

        GameCharacter partnerCharacter = mockCharacter("Sailor Moon");
        TeamMember partnerMembership = new TeamMember(team, mockParticipant(partnerId, "My Partner"));
        partnerMembership.setCharacter(partnerCharacter);

        when(teamMemberRepository.findByParticipantId(meId)).thenReturn(Optional.of(myMembership));
        when(teamMemberRepository.findByTeamId(team.getId())).thenReturn(List.of(myMembership, partnerMembership));

        MyTeamResponse response = service.getMyTeam(meId);

        assertThat(response.myCharacterName()).isEqualTo("Sangoku");
        assertThat(response.partners()).hasSize(1);
        assertThat(response.partners().get(0).displayName()).isEqualTo("My Partner");
        assertThat(response.partners().get(0).characterName()).isEqualTo("Sailor Moon");
    }

    @Test
    void returnsBothPartnersForATrio() {
        UUID meId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        Team team = new Team(event);
        TeamMember myMembership = new TeamMember(team, mockParticipant(meId, "Me"));
        myMembership.setCharacter(mockCharacter("Sangoku"));
        TeamMember partnerOne = new TeamMember(team, mockParticipant(UUID.randomUUID(), "Partner One"));
        partnerOne.setCharacter(mockCharacter("Sailor Moon"));
        TeamMember partnerTwo = new TeamMember(team, mockParticipant(UUID.randomUUID(), "Partner Two"));
        partnerTwo.setCharacter(mockCharacter("Naruto"));

        when(teamMemberRepository.findByParticipantId(meId)).thenReturn(Optional.of(myMembership));
        when(teamMemberRepository.findByTeamId(team.getId()))
                .thenReturn(List.of(myMembership, partnerOne, partnerTwo));

        MyTeamResponse response = service.getMyTeam(meId);

        assertThat(response.partners()).hasSize(2);
        assertThat(response.partners().stream().map(p -> p.characterName()))
                .containsExactlyInAnyOrder("Sailor Moon", "Naruto");
    }

    @Test
    void failsWhenTheParticipantHasNoTeamYet() {
        UUID meId = UUID.randomUUID();
        when(teamMemberRepository.findByParticipantId(meId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyTeam(meId)).isInstanceOf(NotFoundException.class);
    }
}
