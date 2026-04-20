package com.securefiles.secure_file_transfer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  // Frontend routes -> serve index.html so the JS router can take over
  @GetMapping({"/login", "/dashboard", "/profile"})
  public String forwardSpaRoutes() {
    return "forward:/index.html";
  }
}