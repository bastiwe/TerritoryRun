package com.example.areawalker.domain.rules

import com.example.areawalker.domain.model.GpsPoint
import com.example.areawalker.domain.model.ValidationResult

class BackendValidator(
    private val routeValidator: RouteValidator = RouteValidator()
) {
    fun validateSubmittedTrack(points: List<GpsPoint>): ValidationResult {
        // TODO: Move this class to a real backend service and add device integrity, signed samples and replay protection.
        return routeValidator.validate(points)
    }
}

