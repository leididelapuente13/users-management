package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserModel;

public interface CreateUserAccountPort {

  void createAccount(UserModel user);
}
