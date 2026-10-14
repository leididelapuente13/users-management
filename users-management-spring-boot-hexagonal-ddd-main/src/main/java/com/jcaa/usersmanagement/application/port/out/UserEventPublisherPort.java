package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.event.UserCreatedDomainEvent;
import com.jcaa.usersmanagement.domain.event.UserUpdatedDomainEvent;

public interface UserEventPublisherPort {

  void publishUserCreated(UserCreatedDomainEvent event);

  void publishUserUpdated(UserUpdatedDomainEvent event);
}

