package com.expensetrack.repository;

import com.expensetrack.domain.Expense;
import com.expensetrack.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Page<Expense> findAllByUser(User user, Pageable pageable);

    List<Expense> findAllByUser(User user);

    List<Expense> findAllByUserOrderByExpenseDateDesc(User user);

    Optional<Expense> findByIdAndUser(Long id, User user);
}
