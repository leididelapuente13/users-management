package com.jcaa.usersmanagement.infrastructure.adapter.account.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountResponse(
    String id,
    String userId,
    BigDecimal balance,
    String currency,
    String status,
    String createdAt,
    String updatedAt) {}
