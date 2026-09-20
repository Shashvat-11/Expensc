package com.expensetrack.dto.expense;

import com.expensetrack.domain.ExpenseCategory;
import com.expensetrack.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        String description,
        ExpenseCategory category,
        PaymentMethod paymentMethod,
        LocalDate expenseDate,
        LocalDateTime createdAt) {
}
