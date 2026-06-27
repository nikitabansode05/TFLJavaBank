package com.banking.transaction.transactionManagers.transactions;

import java.util.List;

import com.banking.transaction.entities.Transaction;
import com.banking.transaction.entities.Account;

public interface IAccountTransaction {
    Account showAccountDetails(int accountNo);
    boolean createAccount(int accNo,String name);
    List<Transaction> getStatement(int accountNo);
}
