package com.securefiles.secure_file_transfer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  @GetMapping({
      "/",
      "/login",
      "/register",
      "/forgot",
      "/verify",
      "/profile",
      "/dashboard",
      "/shared/{token}"
  })
  public String forward() {
    return "forward:/index.html";
  }
}