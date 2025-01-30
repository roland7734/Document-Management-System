package org.example;

import org.example.interfaces.DocumentClassifier;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class DocumentClassifierImpl extends UnicastRemoteObject implements DocumentClassifier {

    private final AIService aiService;

    public DocumentClassifierImpl(AIService aiService) throws RemoteException {
        super();
        this.aiService = aiService;
    }

    @Override
    public String classify(String documentContent, String existingFolders) throws RemoteException {
        try {
            return aiService.classifyDocument(documentContent, existingFolders);
        } catch (Exception e) {
            throw new RemoteException("Error classifying document", e);
        }
    }
}
