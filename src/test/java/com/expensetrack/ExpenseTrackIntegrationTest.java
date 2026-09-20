package com.expensetrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetrack.domain.ExpenseCategory;
import com.expensetrack.domain.PaymentMethod;
import com.expensetrack.repository.ExpenseRepository;
import com.expensetrack.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseTrackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() throws Exception {
        expenseRepository.deleteAll();
        userRepository.deleteAll();

        registerUser("alice@example.com", "Alice", "password123");
        registerUser("bob@example.com", "Bob", "password123");
        userToken = loginUser("alice@example.com", "password123");
        otherUserToken = loginUser("bob@example.com", "password123");
    }

    @Test
    void registerUser_shouldCreateUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Carol\",\"email\":\"carol@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("carol@example.com"));
    }

    @Test
    void duplicateEmail_shouldReject() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void login_shouldReturnJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void protectedEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createExpense_shouldPersistForAuthenticatedUser() throws Exception {
        String payload = "{\"amount\":450.00,\"description\":\"Dinner\",\"category\":\"FOOD\",\"paymentMethod\":\"UPI\",\"expenseDate\":\"2026-09-20\"}";

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Dinner"));
    }

    @Test
    void getExpenses_shouldReturnUserOnlyExpenses() throws Exception {
        createExpenseAsUser(userToken, "Lunch", 100.00, ExpenseCategory.FOOD, PaymentMethod.UPI, LocalDate.of(2026, 9, 15));
        createExpenseAsUser(otherUserToken, "Taxi", 70.00, ExpenseCategory.TRANSPORT, PaymentMethod.CASH, LocalDate.of(2026, 9, 18));

        mockMvc.perform(get("/api/expenses")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Lunch"));
    }

    @Test
    void updateExpense_shouldModifyOwnedExpense() throws Exception {
        long expenseId = createExpenseAsUser(userToken, "Movie", 200.00, ExpenseCategory.ENTERTAINMENT, PaymentMethod.CREDIT_CARD, LocalDate.of(2026, 9, 10));

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":250.00,\"description\":\"Updated movie\",\"category\":\"ENTERTAINMENT\",\"paymentMethod\":\"CREDIT_CARD\",\"expenseDate\":\"2026-09-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated movie"));
    }

    @Test
    void deleteExpense_shouldDeleteOwnedExpense() throws Exception {
        long expenseId = createExpenseAsUser(userToken, "Groceries", 90.00, ExpenseCategory.FOOD, PaymentMethod.UPI, LocalDate.of(2026, 9, 20));

        mockMvc.perform(delete("/api/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void validationFailure_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"description\":\"\",\"category\":\"FOOD\",\"paymentMethod\":\"UPI\",\"expenseDate\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownershipIsolation_shouldRejectOtherUsersExpenseAccess() throws Exception {
        long expenseId = createExpenseAsUser(userToken, "Alone", 150.00, ExpenseCategory.BILLS, PaymentMethod.BANK_TRANSFER, LocalDate.of(2026, 9, 12));

        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void analytics_shouldComputeAuthenticatedUserSummary() throws Exception {
        createExpenseAsUser(userToken, "Lunch", 100.00, ExpenseCategory.FOOD, PaymentMethod.UPI, LocalDate.of(2026, 9, 15));
        createExpenseAsUser(userToken, "Dinner", 200.00, ExpenseCategory.FOOD, PaymentMethod.UPI, LocalDate.of(2026, 9, 18));
        createExpenseAsUser(userToken, "Train", 50.00, ExpenseCategory.TRANSPORT, PaymentMethod.CASH, LocalDate.of(2026, 8, 10));

        mockMvc.perform(get("/api/expenses/summary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(350.00));

        mockMvc.perform(get("/api/expenses/summary/category")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.FOOD").value(300.00));

        mockMvc.perform(get("/api/expenses/summary/monthly")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['2026-08']").value(50.00));
    }

    private void registerUser(String email, String name, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
    }

    private String loginUser(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> payload = objectMapper.readValue(response, Map.class);
        return (String) payload.get("token");
    }

    private long createExpenseAsUser(String token, String description, double amount, ExpenseCategory category, PaymentMethod method, LocalDate date) throws Exception {
        String payload = "{\"amount\":" + BigDecimal.valueOf(amount) + ",\"description\":\"" + description + "\",\"category\":\"" + category + "\",\"paymentMethod\":\"" + method + "\",\"expenseDate\":\"" + date + "\"}";

        String response = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> data = objectMapper.readValue(response, Map.class);
        return ((Number) data.get("id")).longValue();
    }
}
