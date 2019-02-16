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
public class Account {

    private volatile int balance;
    private final int id;
    private final Bank myBank;
    private final ReentrantLock lock;
    private final Condition notEnoughMoney;
    public Account(Bank myBank, int id, int initialBalance) {
        this.myBank = myBank;
        this.id = id;
        balance = initialBalance;
        lock = new ReentrantLock();
        notEnoughMoney = lock.newCondition();
    }

    public int getBalance() {
        return balance;
    }

    public void withdraw(int amount) {
        lock.lock();
        try {
            while(amount > balance){
                notEnoughMoney.await();
            }
        myBank.decrementSafeThreadsCount();
        int currentBalance = balance;
     //   Thread.yield(); // Try to force collision
        int newBalance = currentBalance - amount;
        balance = newBalance;
        } catch (InterruptedException ex) {
            Logger.getLogger(Account.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            lock.unlock();
        }

    }

    public void deposit(int amount) {
        lock.lock();
        try{
        int currentBalance = balance;
       // Thread.yield();   // Try to force collision
        int newBalance = currentBalance + amount;
        balance = newBalance;
        myBank.incrementSafeThreadsCount();
        notEnoughMoney.signal();
        }finally{
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return String.format("Account[%d] balance %d", id, balance);
    }
}
