package com.ceramic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryController {

    @GetMapping("hi")
    public String hi(){
        return "ahihi";
    }
}
