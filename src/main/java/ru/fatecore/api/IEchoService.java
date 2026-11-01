package ru.fatecore.api;

import java.util.UUID;

public interface IEchoService {
    long getBalance(UUID id);
    boolean withdraw(UUID id, long amount);
    void give(UUID id, long amount);
}
