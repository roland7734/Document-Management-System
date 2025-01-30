package org.example.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DocumentSummarizer extends Remote {
    String summarize(String documentContent) throws RemoteException;
}