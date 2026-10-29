package com.example.shopping.tutor.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TutorPage {
    @GetMapping({"/","/tutor","/tutor/"}) public String index() { return "forward:/tutor/index.html"; }
}
