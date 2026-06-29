package com.material.auth.dto.business;

import java.util.List;

public record FulfillmentRankingsView(
        List<ParticipantRankingView> purchasers,
        List<ParticipantRankingView> suppliers,
        List<ParticipantRankingView> drivers
) {
}
