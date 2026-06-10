package com.material.auth.service.geo;

import java.util.Optional;

public interface GeocodingService {
    Optional<Coordinates> resolve(String address);
}
