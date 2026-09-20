package com.expensetrack.controller;

import com.expensetrack.domain.ExpenseCategory;
import com.expensetrack.domain.PaymentMethod;
import com.expensetrack.dto.expense.ExpenseRequest;
import com.expensetrack.dto.expense.ExpenseResponse;
import com.expensetrack.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Expenses", description = "Expense CRUD and analytics")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/expenses")
    @Operation(summary = "Create a new expense for the authenticated user")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(request));
    }

    @GetMapping("/expenses")
    @Operation(summary = "List expenses for the authenticated user with filtering and pagination")
    public ResponseEntity<Page<ExpenseResponse>> getExpenses(
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate,desc") String sort) {

        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 10;
        }

        return ResponseEntity.ok(expenseService.getExpenses(category, paymentMethod, from, to, minAmount, maxAmount, search, page, size, sort));
    }

    @GetMapping("/expenses/{id}")
    @Operation(summary = "Get one expense by id for the authenticated user")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PutMapping("/expenses/{id}")
    @Operation(summary = "Update one expense belonging to the authenticated user")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @DeleteMapping("/expenses/{id}")
    @Operation(summary = "Delete one expense belonging to the authenticated user")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expenses/summary")
    @Operation(summary = "Get aggregate expense summary for the authenticated user")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(expenseService.getSummary());
    }

    @GetMapping("/expenses/summary/category")
    @Operation(summary = "Get grouped expense totals by category for the authenticated user")
    public ResponseEntity<Map<String, BigDecimal>> getSummaryByCategory() {
        return ResponseEntity.ok(expenseService.getSummaryByCategory());
    }

    @GetMapping("/expenses/summary/monthly")
    @Operation(summary = "Get grouped expense totals by month for the authenticated user")
    public ResponseEntity<Map<String, BigDecimal>> getSummaryByMonth() {
        return ResponseEntity.ok(expenseService.getSummaryByMonth());
    }
}
