package edu.temple.cis.c3238.banksim;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Cay Horstmann
 * @author Modified by Paul Wolfgang
 * @author Modified by Charles Wang
 */
public class Bank {

    public static final int NTEST = 10;
    private final Account[] accounts;
    private long ntransacts = 0;
    private final int initialBalance;
    private final int numAccounts;
    private final ReentrantLock transactionCountLock;
    private final ReentrantLock safeThreadsCountLock;
    private final Condition allThreadsSafe;
    private int safeThreadsCount;

    public Bank(int numAccounts, int initialBalance) {
        this.initialBalance = initialBalance;
        this.numAccounts = numAccounts;
        accounts = new Account[numAccounts];
        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new Account(this, i, initialBalance);
        }
        ntransacts = 0;
        safeThreadsCount = 10;
        transactionCountLock = new ReentrantLock();
        safeThreadsCountLock = new ReentrantLock();
        allThreadsSafe = safeThreadsCountLock.newCondition();
    }

    public void transfer(int from, int to, int amount) {

        shouldTest();

        decrementSafeThreadsCount();
        accounts[from].withdraw(amount);
        accounts[to].deposit(amount);
        incrementSafeThreadsCount();
    }
    
    public void incrementSafeThreadsCount(){
        safeThreadsCountLock.lock();
        try {
            safeThreadsCount++;
            allThreadsSafe.signal();

        } finally {
            safeThreadsCountLock.unlock();
        }
    }
    
    public void decrementSafeThreadsCount(){
         safeThreadsCountLock.lock();
        try {
            safeThreadsCount--;
        } finally {
            safeThreadsCountLock.unlock();
        }

    }
    public void test() {
        int sum = 0;
        for (Account account : accounts) {
//            System.out.printf("%s %s%n",
//                    Thread.currentThread().toString(), account.toString());
            sum += account.getBalance();
        }
//        System.out.println(Thread.currentThread().toString()
//                + " Sum: " + sum);
        if (sum != numAccounts * initialBalance) {
            System.out.println(Thread.currentThread().toString()
                    + " Money was gained or lost");
            System.exit(1);
        } else {
            System.out.println(Thread.currentThread().toString()
                    + " The bank is in balance");
        }
    }

    public int size() {
        return accounts.length;
    }

    public void shouldTest() {
        transactionCountLock.lock();
        try {
            if (++ntransacts % NTEST == 0) {
                //WE MUST TEST
                safeThreadsCountLock.lock();
                try {
                    while (safeThreadsCount < numAccounts) {
                        allThreadsSafe.await();
                    }
                    test();
                    System.out.printf("Number of transactions so far: %d\n", ntransacts);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Bank.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    safeThreadsCountLock.unlock();
                }
            }
        } finally {
            transactionCountLock.unlock();
        }
    }

}
