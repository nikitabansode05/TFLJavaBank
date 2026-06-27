package com.banking.transaction.repositories.accounts;

import java.util.List;

import com.banking.transaction.entities.Account;

public interface AccountFileIOManager{
    void serialize(List<Account> accounts);
    List<Account> deserialize();
   
}
