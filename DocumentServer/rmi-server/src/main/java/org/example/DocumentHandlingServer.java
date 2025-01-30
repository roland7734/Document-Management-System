package org.example;

import org.example.interfaces.DocumentClassifier;
import org.example.interfaces.DocumentSummarizer;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class DocumentHandlingServer {

    public static void main(String[] args) {
        try {
            String serverIp = "192.168.56.1";
            int rmiPort = 1099;

            LocateRegistry.createRegistry(rmiPort);

            AIService aiService = new AIService();

            DocumentClassifier classifier = new DocumentClassifierImpl(aiService);
            DocumentSummarizer summarizer = new DocumentSummarizerImpl(aiService);

            Naming.rebind("rmi://" + serverIp + ":" + rmiPort + "/DocumentClassifier", classifier);
            Naming.rebind("rmi://" + serverIp + ":" + rmiPort + "/DocumentSummarizer", summarizer);

            System.out.println("Document Handling RMI Server started on " + serverIp + ":" + rmiPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}