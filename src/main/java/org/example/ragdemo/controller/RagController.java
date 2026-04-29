package org.example.ragdemo.controller;

import org.example.ragdemo.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    @Autowired
    private RagService ragService;
    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String message){
        Flux<String> result=ragService.chat(message);
        return result;
    }

}
