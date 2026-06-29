package com.material.auth.dto.business;

public record ParticipantRankingView(
        Long participantId,
        String displayName,
        String ratingScore,
        Integer rank
) {
}
