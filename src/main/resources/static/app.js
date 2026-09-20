const API_BASE = '/api';
let token = localStorage.getItem('expenseTrackToken') || '';
let currentPage = 0;
let pageSize = 10;
let selectedExpenseId = null;

const authSection = document.getElementById('authSection');
const dashboardSection = document.getElementById('dashboardSection');
const logoutBtn = document.getElementById('logoutBtn');
const summaryCards = document.getElementById('summaryCards');
const expenseTableBody = document.getElementById('expenseTableBody');
const categorySummary = document.getElementById('categorySummary');
const monthlySummary = document.getElementById('monthlySummary');
const pageInfo = document.getElementById('pageInfo');

const setAuthState = () => {
  if (token) {
    authSection.classList.add('hidden');
    dashboardSection.classList.remove('hidden');
    logoutBtn.classList.remove('hidden');
    loadDashboard();
  } else {
    authSection.classList.remove('hidden');
    dashboardSection.classList.add('hidden');
    logoutBtn.classList.add('hidden');
  }
};

const registerForm = document.getElementById('registerForm');
registerForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const payload = {
    name: document.getElementById('registerName').value,
    email: document.getElementById('registerEmail').value,
    password: document.getElementById('registerPassword').value,
  };

  try {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Registration failed');
    }

    token = data.token;
    localStorage.setItem('expenseTrackToken', token);
    setAuthState();
    registerForm.reset();
  } catch (error) {
    alert(error.message);
  }
});

const loginForm = document.getElementById('loginForm');
loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const payload = {
    email: document.getElementById('loginEmail').value,
    password: document.getElementById('loginPassword').value,
  };

  try {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Login failed');
    }

    token = data.token;
    localStorage.setItem('expenseTrackToken', token);
    setAuthState();
    loginForm.reset();
  } catch (error) {
    alert(error.message);
  }
});

logoutBtn.addEventListener('click', () => {
  token = '';
  localStorage.removeItem('expenseTrackToken');
  setAuthState();
});

const fetchJson = async (url, options = {}) => {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });

  if (response.status === 401) {
    token = '';
    localStorage.removeItem('expenseTrackToken');
    setAuthState();
    throw new Error('Session expired. Please log in again.');
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(data?.message || 'Request failed');
  }

  return data;
};

const loadDashboard = async () => {
  await Promise.all([
    loadSummary(),
    loadExpenses(),
    loadCategorySummary(),
    loadMonthlySummary(),
  ]);
};

const loadSummary = async () => {
  const data = await fetchJson(`${API_BASE}/expenses/summary`);
  summaryCards.innerHTML = `
    <div class="summary-card"><div class="label">Total</div><div class="value">₹${Number(data.totalExpense || 0).toFixed(2)}</div></div>
    <div class="summary-card"><div class="label">Average</div><div class="value">₹${Number(data.averageExpense || 0).toFixed(2)}</div></div>
    <div class="summary-card"><div class="label">Highest</div><div class="value">₹${Number(data.highestExpense || 0).toFixed(2)}</div></div>
    <div class="summary-card"><div class="label">Count</div><div class="value">${data.expenseCount || 0}</div></div>
  `;
};

const loadCategorySummary = async () => {
  const data = await fetchJson(`${API_BASE}/expenses/summary/category`);
  const entries = Object.entries(data || {});
  categorySummary.innerHTML = entries.length ? entries.map(([key, value]) => `
    <li><span>${key}</span><strong>₹${Number(value).toFixed(2)}</strong></li>
  `).join('') : '<li>No category data</li>';
};

const loadMonthlySummary = async () => {
  const data = await fetchJson(`${API_BASE}/expenses/summary/monthly`);
  const entries = Object.entries(data || {});
  monthlySummary.innerHTML = entries.length ? entries.map(([key, value]) => `
    <li><span>${key}</span><strong>₹${Number(value).toFixed(2)}</strong></li>
  `).join('') : '<li>No monthly data</li>';
};

const buildQueryString = () => {
  const params = new URLSearchParams();
  const category = document.getElementById('filterCategory').value;
  const paymentMethod = document.getElementById('filterPaymentMethod').value;
  const from = document.getElementById('filterFrom').value;
  const to = document.getElementById('filterTo').value;
  const minAmount = document.getElementById('filterMinAmount').value;
  const maxAmount = document.getElementById('filterMaxAmount').value;
  const search = document.getElementById('filterSearch').value;

  if (category) params.append('category', category);
  if (paymentMethod) params.append('paymentMethod', paymentMethod);
  if (from) params.append('from', from);
  if (to) params.append('to', to);
  if (minAmount) params.append('minAmount', minAmount);
  if (maxAmount) params.append('maxAmount', maxAmount);
  if (search) params.append('search', search);
  params.append('page', String(currentPage));
  params.append('size', String(pageSize));
  params.append('sort', 'expenseDate,desc');
  return params.toString();
};

const loadExpenses = async () => {
  const query = buildQueryString();
  const data = await fetchJson(`${API_BASE}/expenses?${query}`);
  const list = data.content || [];
  pageInfo.textContent = `Page ${data.number || 0}`;

  if (!list.length) {
    expenseTableBody.innerHTML = '<tr><td colspan="6">No expenses found.</td></tr>';
    document.getElementById('prevPageBtn').disabled = data.number <= 0;
    document.getElementById('nextPageBtn').disabled = data.last;
    return;
  }

  expenseTableBody.innerHTML = list.map((expense) => `
    <tr>
      <td>${expense.expenseDate}</td>
      <td>${expense.description}</td>
      <td>${expense.category}</td>
      <td>${expense.paymentMethod}</td>
      <td>₹${Number(expense.amount).toFixed(2)}</td>
      <td>
        <div class="actions">
          <button class="btn secondary" type="button" data-edit-id="${expense.id}">Edit</button>
          <button class="btn danger" type="button" data-delete-id="${expense.id}">Delete</button>
        </div>
      </td>
    </tr>
  `).join('');

  document.getElementById('prevPageBtn').disabled = data.number <= 0;
  document.getElementById('nextPageBtn').disabled = data.last;

  expenseTableBody.querySelectorAll('[data-edit-id]').forEach((button) => {
    button.addEventListener('click', () => populateExpenseForm(Number(button.dataset.editId)));
  });

  expenseTableBody.querySelectorAll('[data-delete-id]').forEach((button) => {
    button.addEventListener('click', () => deleteExpense(Number(button.dataset.deleteId)));
  });
};

const populateExpenseForm = async (id) => {
  const expense = await fetchJson(`${API_BASE}/expenses/${id}`);
  selectedExpenseId = id;
  document.getElementById('expenseAmount').value = expense.amount;
  document.getElementById('expenseDescription').value = expense.description;
  document.getElementById('expenseCategory').value = expense.category;
  document.getElementById('expensePaymentMethod').value = expense.paymentMethod;
  document.getElementById('expenseDate').value = expense.expenseDate;
  const submitBtn = document.querySelector('#expenseForm button[type="submit"]');
  submitBtn.textContent = 'Update Expense';
};

const deleteExpense = async (id) => {
  if (!confirm('Delete this expense?')) return;
  await fetchJson(`${API_BASE}/expenses/${id}`, { method: 'DELETE' });
  selectedExpenseId = null;
  const submitBtn = document.querySelector('#expenseForm button[type="submit"]');
  submitBtn.textContent = 'Save Expense';
  expenseForm.reset();
  loadDashboard();
};

const expenseForm = document.getElementById('expenseForm');
expenseForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const payload = {
    amount: Number(document.getElementById('expenseAmount').value),
    description: document.getElementById('expenseDescription').value,
    category: document.getElementById('expenseCategory').value,
    paymentMethod: document.getElementById('expensePaymentMethod').value,
    expenseDate: document.getElementById('expenseDate').value,
  };

  try {
    const url = selectedExpenseId ? `${API_BASE}/expenses/${selectedExpenseId}` : `${API_BASE}/expenses`;
    const method = selectedExpenseId ? 'PUT' : 'POST';
    await fetchJson(url, {
      method,
      body: JSON.stringify(payload),
    });

    selectedExpenseId = null;
    expenseForm.reset();
    const submitBtn = document.querySelector('#expenseForm button[type="submit"]');
    submitBtn.textContent = 'Save Expense';
    loadDashboard();
  } catch (error) {
    alert(error.message);
  }
});

document.getElementById('applyFiltersBtn').addEventListener('click', () => {
  currentPage = 0;
  loadExpenses();
});

document.getElementById('clearFiltersBtn').addEventListener('click', () => {
  document.getElementById('filterCategory').value = '';
  document.getElementById('filterPaymentMethod').value = '';
  document.getElementById('filterFrom').value = '';
  document.getElementById('filterTo').value = '';
  document.getElementById('filterMinAmount').value = '';
  document.getElementById('filterMaxAmount').value = '';
  document.getElementById('filterSearch').value = '';
  currentPage = 0;
  loadExpenses();
});

document.getElementById('prevPageBtn').addEventListener('click', () => {
  if (currentPage > 0) {
    currentPage -= 1;
    loadExpenses();
  }
});

document.getElementById('nextPageBtn').addEventListener('click', () => {
  currentPage += 1;
  loadExpenses();
});

document.querySelectorAll('.tab').forEach((tab) => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach((item) => item.classList.remove('active'));
    tab.classList.add('active');
    document.querySelectorAll('.form-panel').forEach((panel) => panel.classList.remove('active'));
    document.getElementById(tab.dataset.target).classList.add('active');
  });
});

setAuthState();
