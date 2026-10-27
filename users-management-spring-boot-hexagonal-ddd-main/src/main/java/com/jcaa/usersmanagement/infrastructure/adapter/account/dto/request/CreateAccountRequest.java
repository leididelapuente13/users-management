package com.jcaa.usersmanagement.infrastructure.adapter.account.dto.request;

public record CreateAccountRequest(
    String userId,
    String currency,
    String name,
    String email) {}
