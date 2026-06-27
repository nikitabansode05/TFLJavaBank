package com.banking.transaction.transactionManagers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.banking.transaction.entities.Account;
import com.banking.transaction.entities.Transaction;
import com.banking.transaction.listeners.AccountListener;
import com.banking.transaction.listeners.AccountListenerImpl;
import com.banking.transaction.repositories.accounts.AccountFileIOManager;
import com.banking.transaction.repositories.accounts.AccountFileIOManagerImpl;
import com.banking.transaction.repositories.transactions.TransactionsFileIOManager;
import com.banking.transaction.repositories.transactions.TransactionsFileIOManagerImpl;
import com.banking.transaction.transactionManagers.transactions.IAccountTransaction;
import com.banking.transaction.transactionManagers.transactions.IDepositTransaction;
import com.banking.transaction.transactionManagers.transactions.IWithdrawTransaction;
import com.banking.transaction.transactionManagers.transactions.IFundTransfer;
import com.banking.transaction.transactionManagers.transactions.IInterestTransaction;

@Component
public class AccountDepartment implements IDepositTransaction,IWithdrawTransaction,IAccountTransaction,IFundTransfer,IInterestTransaction{

    @Override
    public boolean credit(double creditAmount, int accountNo) {
        AccountFileIOManager accountSerializer = new AccountFileIOManagerImpl();
        TransactionsFileIOManager transactionSerializer = new TransactionsFileIOManagerImpl();
        AccountListener listener = new AccountListenerImpl();

        try {
            List<Account> accounts = accountSerializer.deserialize();
            List<Transaction> transactions = transactionSerializer.deserialize();

            Account account = accounts.stream().filter(a -> a.getAccountNo() == accountNo).findFirst().orElse(null);

            if (account == null)
                return false;

            double newBalance = account.getBalance() + creditAmount;
            account.setBalance(newBalance);

            transactions.add(new Transaction(accountNo,"credited",LocalDateTime.now(),creditAmount,newBalance));

            transactionSerializer.serialize(transactions);
            accountSerializer.serialize(accounts);
            listener.onCredit(creditAmount, newBalance);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }   

    @Override
    public boolean debit(double debitAmount,int accountNo){
        AccountFileIOManager accountSerializer=new AccountFileIOManagerImpl();
        TransactionsFileIOManager transactionSerializer=new TransactionsFileIOManagerImpl();
        AccountListener listener=new AccountListenerImpl();

        try{
            List<Account> accountList=accountSerializer.deserialize();
            List<Transaction> transactions=transactionSerializer.deserialize();
           
            Account account = accountList.stream().filter(a -> a.getAccountNo() == accountNo).findFirst().orElse(null);

            double newBalance=account.getBalance();
            newBalance-=debitAmount;
            account.setBalance(newBalance);

            transactions.add(new Transaction(account.getAccountNo(),"debited",LocalDateTime.now(),debitAmount,account.getBalance()));
           
            transactionSerializer.serialize(transactions);
            accountSerializer.serialize(accountList);
            listener.onDebit(debitAmount,newBalance);
            
            return true;

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Account showAccountDetails(int accountNo){
        AccountFileIOManager accountSerializer=new AccountFileIOManagerImpl();
        try{
            List<Account> accountList=accountSerializer.deserialize();
            Account account=accountList.stream().filter(a->a.getAccountNo()==accountNo).findFirst().orElse(null);                  
            return account;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean createAccount(int accountNo,String name){
        AccountFileIOManager accountSerializer=new AccountFileIOManagerImpl();
        List<Account> accountList=accountSerializer.deserialize();
        Account account = new Account(accountNo,name,0.0,LocalDateTime.now());
        try{
            accountList.add(account);
            accountSerializer.serialize(accountList);
            return true;
        }catch(Exception e){
        e.printStackTrace();
        return false;
      }
    }

   @Override
    public List<Transaction> getStatement(int accountNo) {
        TransactionsFileIOManager transactionSerializer = new TransactionsFileIOManagerImpl();
        List<Transaction> transactions = transactionSerializer.deserialize();
        List<Transaction> acctransactions = transactions.stream().filter(o -> o.getAccountNo() == accountNo).toList();
        int count = acctransactions.size();
        System.out.println(count);
        if (count > 5) {
            return acctransactions.subList(count - 5, count);
        }
        return acctransactions;
    }

    @Override
    public boolean transaction(int accountNo1,int accountNo2,double amount){
        try{
            AccountFileIOManager accountSerializer=new AccountFileIOManagerImpl();
            List<Account> accountList=accountSerializer.deserialize();
            accountList.stream().filter(a -> a.getAccountNo() == accountNo1).forEach(a -> debit(amount, accountNo1));
            accountList.stream().filter(a -> a.getAccountNo() == accountNo2).forEach(a -> credit(amount, accountNo2));
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public double applyInterest(int accountNo, double interest) {
        AccountFileIOManager accountSerializer = new AccountFileIOManagerImpl();
        TransactionsFileIOManager transactionSerializer = new TransactionsFileIOManagerImpl();
        List<Account> accounts = accountSerializer.deserialize();
        List<Transaction> transactions = transactionSerializer.deserialize();
        List<Transaction> log = transactions.stream().filter(o -> o.getAccountNo() == accountNo).toList();   
        if (log.isEmpty()) {
            double accountInterest=applyInterestToAccountWithoutTransaction(accountNo, interest);
            if(accountInterest>0.0){
                credit(accountInterest,accountNo);
            }
            return accountInterest;
        }
        double totalInterest = 0;
        for (int i = 0; i < log.size() - 1; i++) {
            totalInterest += interestCalculation(log.get(i).getDatetime().toLocalDate(),log.get(i + 1).getDatetime().toLocalDate(),interest,log.get(i).getBalance());
        }
        double currentBalance = accounts.stream().filter(a -> a.getAccountNo() == accountNo).findFirst().map(Account::getBalance).orElse(0.0);
        totalInterest += interestCalculation(log.get(log.size() - 1).getDatetime().toLocalDate(),LocalDate.now(),interest,currentBalance);
        credit(totalInterest, accountNo);
        return totalInterest;
    }

    @Override
    public void applyInteresttoAll(double interest){  
        AccountFileIOManager accountFile=new AccountFileIOManagerImpl();
        List<Account> accountList=accountFile.deserialize();
        accountList.forEach(a -> applyInterest(a.getAccountNo(), interest));
    }

    @Override
    public double interestCalculation(LocalDate fromdate,LocalDate todate,double interest,double balance){
        long days = ChronoUnit.DAYS.between(fromdate, todate);
        double principleAmount=balance;
        double base=1+((interest/100)/365);
        double power=days;
        double calculateBasePower=Math.pow(base, power);
        double finaAmount= principleAmount*(calculateBasePower);
        double calculatedInterest = finaAmount-principleAmount; 
        return calculatedInterest;
    }

    public double applyInterestToAccountWithoutTransaction(int accountNo,double interest){
        AccountFileIOManager accountFile=new AccountFileIOManagerImpl();
        List<Account> accountList=accountFile.deserialize();
        double calculatedInterest=0;
        for(Account a:accountList){
            if(a.getAccountNo()==accountNo){
                LocalDate startDate=a.getDatetime().toLocalDate();
                System.out.println(startDate);
                LocalDate endDate=LocalDate.now();
                System.out.println(endDate);
                calculatedInterest=interestCalculation(startDate, endDate, interest, a.getBalance());
            }
        }
        return calculatedInterest;
    }

}
