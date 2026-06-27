package com.banking.transaction.repositories.transactions;

import com.banking.transaction.entities.Transaction;
import java.util.List;

public interface TransactionsFileIOManager {
    void serialize(List<Transaction> transactions);
    List<Transaction> deserialize();
}
