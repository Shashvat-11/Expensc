package com.expensetrack.service;

import com.expensetrack.domain.Expense;
import com.expensetrack.domain.ExpenseCategory;
import com.expensetrack.domain.PaymentMethod;
import com.expensetrack.domain.User;
import com.expensetrack.dto.expense.ExpenseRequest;
import com.expensetrack.dto.expense.ExpenseResponse;
import com.expensetrack.exception.ResourceNotFoundException;
import com.expensetrack.repository.ExpenseRepository;
import com.expensetrack.repository.ExpenseSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;

    public ExpenseService(ExpenseRepository expenseRepository, UserService userService) {
        this.expenseRepository = expenseRepository;
        this.userService = userService;
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = userService.getCurrentUser();
        Expense expense = Expense.builder()
                .amount(request.amount())
                .description(request.description().trim())
                .category(request.category())
                .paymentMethod(request.paymentMethod())
                .expenseDate(request.expenseDate())
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(
            ExpenseCategory category,
            PaymentMethod paymentMethod,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            int page,
            int size,
            String sort) {

        User user = userService.getCurrentUser();
        Sort sortSpec = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        Specification<Expense> spec = ExpenseSpecifications.withFilters(category, paymentMethod, from, to, minAmount, maxAmount, search, user);
        return expenseRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        User user = userService.getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = userService.getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        expense.setAmount(request.amount());
        expense.setDescription(request.description().trim());
        expense.setCategory(request.category());
        expense.setPaymentMethod(request.paymentMethod());
        expense.setExpenseDate(request.expenseDate());

        Expense updated = expenseRepository.save(expense);
        return toResponse(updated);
    }

    @Transactional
    public void deleteExpense(Long id) {
        User user = userService.getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSummaryByCategory() {
        User user = userService.getCurrentUser();
        return expenseRepository.findAllByUser(user).stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory().name(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSummaryByMonth() {
        User user = userService.getCurrentUser();
        return expenseRepository.findAllByUser(user).stream()
                .collect(Collectors.groupingBy(
                        expense -> YearMonth.from(expense.getExpenseDate()).toString(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {
        User user = userService.getCurrentUser();
        List<Expense> expenses = expenseRepository.findAllByUser(user);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = expenses.isEmpty() ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(expenses.size()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal highest = expenses.stream()
                .map(Expense::getAmount)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalExpense", total);
        summary.put("averageExpense", average);
        summary.put("highestExpense", highest);
        summary.put("expenseCount", expenses.size());
        return summary;
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "expenseDate");
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(direction, property);
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getPaymentMethod(),
                expense.getExpenseDate(),
                expense.getCreatedAt());
    }
}
