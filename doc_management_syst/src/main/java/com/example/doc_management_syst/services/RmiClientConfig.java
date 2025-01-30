package com.example.doc_management_syst.services;

import org.example.interfaces.DocumentClassifier;
import org.example.interfaces.DocumentSummarizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.rmi.Naming;

@Configuration
public class RmiClientConfig {

    @Bean
    public DocumentClassifier documentClassifier() throws Exception {
        String serverIp = "192.168.56.1";
        int rmiPort = 1099;
        String rmiUrl = "rmi://" + serverIp + ":" + rmiPort + "/DocumentClassifier";
        return (DocumentClassifier) Naming.lookup(rmiUrl);
    }

    @Bean
    public DocumentSummarizer documentSummarizer() throws Exception {
        String serverIp = "192.168.56.1";
        int rmiPort = 1099;
        String rmiUrl = "rmi://" + serverIp + ":" + rmiPort + "/DocumentSummarizer";
        return (DocumentSummarizer) Naming.lookup(rmiUrl);
    }
}
