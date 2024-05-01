package com.scaler.splitwise_mar22.strategies.settleup;

import com.scaler.splitwise_mar22.models.Expense;
import com.scaler.splitwise_mar22.models.Transaction;
import com.scaler.splitwise_mar22.models.User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GreedySettleUpExpenseStrategy implements SettleUpExpensesStrategy {

    class Record {
        User user;
        int pendingAmount;

        Record(User user, int pendingAmount) {
            this.user = user;
            this.pendingAmount = pendingAmount;
        }
    }

    @Override
    public List<Transaction> settle(List<Expense> expenses) {
        // Note that this method doesn't care about whether the expenses belong to a group or not
        //Caller of this method will make sure to pass Group expenses if Settle up request is for a group
        // Step 1: Calculate amount for each user
        Map<User, Integer> extraMoney = new HashMap<>();

        for (Expense expense: expenses) {
            for (User user: expense.getPaidBy().keySet()) {
                if (!extraMoney.containsKey(user)) {
                    extraMoney.put(user, 0);
                }

                extraMoney.put(user, extraMoney.get(user) + expense.getPaidBy().get(user));
            }

            for (User user: expense.getPaidBy().keySet()) {
                if (!extraMoney.containsKey(user)) {
                    extraMoney.put(user, 0);
                }

                extraMoney.put(user, extraMoney.get(user) - expense.getPaidBy().get(user));
            }
        }

        // Step2: Create a min heap for negative values and max heap for positive values.
        PriorityQueue<Record> negativeQueue = new PriorityQueue<>((r1, r2) -> r1.pendingAmount - r2.pendingAmount);
        PriorityQueue<Record> positiveQueue = new PriorityQueue<>((r1, r2) -> r2.pendingAmount - r1.pendingAmount);

        for (User user: extraMoney.keySet()) {
            if (extraMoney.get(user) < 0) {
                negativeQueue.add(new Record(user, extraMoney.get(user)));
            } else if (extraMoney.get(user) > 0) {
                positiveQueue.add(new Record(user, extraMoney.get(user)));
            }
        }

        List<Transaction> transactions = new ArrayList<>();

        // A: 20
        // B: -80
        while (!positiveQueue.isEmpty() && !negativeQueue.isEmpty()) {
            Record firstNegative = negativeQueue.remove();
            Record firstPostive = positiveQueue.remove();

            if (firstPostive.pendingAmount > Math.abs(firstNegative.pendingAmount)) {
                // If positive amount is more, we will have to push the positive value back to positiveQueue
                // from will always be the user with negative amount
                transactions.add(
                        new Transaction(firstNegative.user.toDto(), firstPostive.user.toDto(), Math.abs(firstNegative.pendingAmount))
                );
                positiveQueue.add(new Record(firstPostive.user, firstPostive.pendingAmount - Math.abs(firstNegative.pendingAmount)));
            } else {
                // If negative amount is more, we will have to push the remaining negative value back to negativeQueue
                transactions.add(
                        new Transaction(firstNegative.user.toDto(), firstPostive.user.toDto(), firstPostive.pendingAmount)
                );
                negativeQueue.add(new Record(firstNegative.user, firstNegative.pendingAmount + firstPostive.pendingAmount));
            }
        }

        return transactions;
    }
}

// 1. Code

// expense 1
// paidBy: 1 - 50   2 - 100
// owedBy: 1 - 25   2 - 25  3 - 25 4 - 75

// expense 2
// paidBy: 2 - 50   2 - 10
// owedBy: 1 - 15   2 - 15  3 - 15 4 - 15
