package org.example.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DocumentClassifier extends Remote {
    String classify(String documentContent, String existingFolders) throws RemoteException;
}