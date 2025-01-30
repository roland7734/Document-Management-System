package com.example.doc_management_syst.controllers;

import org.example.interfaces.DocumentClassifier;
import org.example.interfaces.DocumentSummarizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.rmi.RemoteException;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentClassifier documentClassifier;

    @Autowired
    private DocumentSummarizer documentSummarizer;

    @PostMapping("/classify")
    public String classifyDocument(@RequestParam String content, @RequestParam String folders) {
        try {
            return documentClassifier.classify(content, folders);
        } catch (RemoteException e) {
            throw new RuntimeException("Error classifying document", e);
        }
    }

    @PostMapping("/summarize")
    public String summarizeDocument(@RequestParam String content) {
        try {
            return documentSummarizer.summarize(content);
        } catch (RemoteException e) {
            throw new RuntimeException("Error summarizing document", e);
        }
    }
}
