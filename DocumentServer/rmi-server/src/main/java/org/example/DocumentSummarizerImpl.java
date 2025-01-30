package org.example;

import org.example.interfaces.DocumentSummarizer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class DocumentSummarizerImpl extends UnicastRemoteObject implements DocumentSummarizer {

    private final AIService aiService;

    public DocumentSummarizerImpl(AIService aiService) throws RemoteException {
        super();
        this.aiService = aiService;
    }

    @Override
    public String summarize(String documentContent) throws RemoteException {
        try {
            return aiService.summarizeDocument(documentContent);
        } catch (Exception e) {
            throw new RemoteException("Error summarizing document", e);
        }
    }
}
