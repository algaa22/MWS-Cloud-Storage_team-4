package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import java.util.List;

public record AvailableTariffsResponse(List<TariffPlanResponse> tariffs) {}
