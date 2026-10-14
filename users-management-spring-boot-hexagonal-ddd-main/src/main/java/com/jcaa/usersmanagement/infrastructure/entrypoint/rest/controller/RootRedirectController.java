package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class RootRedirectController {

  @GetMapping("/")
  public String redirectToSwagger() {
    return "redirect:/swagger-ui/index.html";
  }
}

