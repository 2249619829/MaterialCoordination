package com.material.auth.dto.business;

public record DriverAttendanceView(
        Long driverId,
        String date,
        boolean online
) {
}
