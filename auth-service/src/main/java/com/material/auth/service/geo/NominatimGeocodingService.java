package com.material.auth.service.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class NominatimGeocodingService implements GeocodingService {
    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingService.class);

    private final RestClient restClient;

    public NominatimGeocodingService(@Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "material-coordination-platform/0.1")
                .build();
    }

    @Override
    public Optional<Coordinates> resolve(String address) {
        String normalized = address == null ? "" : address.trim();
        if (!StringUtils.hasText(normalized) || "待完善".equals(normalized)) {
            return Optional.empty();
        }
        try {
            List<NominatimResult> results = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .queryParam("addressdetails", "0")
                            .queryParam("countrycodes", "cn")
                            .queryParam("accept-language", "zh-CN")
                            .queryParam("q", normalized)
                            .build())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {
                    });
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }
            NominatimResult first = results.getFirst();
            BigDecimal longitude = coordinate(first.lon());
            BigDecimal latitude = coordinate(first.lat());
            if (longitude == null || latitude == null) {
                return Optional.empty();
            }
            return Optional.of(new Coordinates(longitude, latitude));
        } catch (RuntimeException exception) {
            log.warn("geocoding failed for address={}", normalized, exception);
            return Optional.empty();
        }
    }

    private BigDecimal coordinate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim()).setScale(6, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record NominatimResult(String lon, String lat) {
    }
}
