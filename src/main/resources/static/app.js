const API = (window.API_BASE || 'http://localhost:8080') + '/api/loans';
let currentAppId = null;

document.addEventListener('DOMContentLoaded', () => {
  bindTabs();
  bindButtons();
  bindModal();
  bindKeyboard();
});

function bindTabs() {
  document.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => switchTab(button.dataset.tab));
  });
}

function bindButtons() {
  document.getElementById('btn-submit').addEventListener('click', submitApplication);
  document.getElementById('btn-load').addEventListener('click', loadApplication);
  document.getElementById('btn-approve').addEventListener('click', approveApplication);
  document.getElementById('btn-reject').addEventListener('click', openRejectModal);
  document.getElementById('btn-confirm-reject').addEventListener('click', confirmReject);
  document.getElementById('btn-cancel-reject').addEventListener('click', closeRejectModal);
  document.getElementById('submit-alert').addEventListener('click', handleAlertAction);
}

function bindModal() {
  document.getElementById('reject-modal').addEventListener('click', (event) => {
    if (event.target === event.currentTarget) {
      closeRejectModal();
    }
  });
}

function bindKeyboard() {
  document.getElementById('r-id').addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      loadApplication();
    }
  });
}

function switchTab(tab) {
  document.querySelectorAll('.page').forEach((page) => page.classList.remove('active'));
  document.querySelectorAll('.nav-tab').forEach((tabButton) => tabButton.classList.remove('active'));

  document.getElementById(`page-${tab}`).classList.add('active');
  document.querySelector(`.nav-tab[data-tab="${tab}"]`).classList.add('active');
}

function handleAlertAction(event) {
  const button = event.target.closest('[data-open-review]');
  if (button) {
    openReview(button.dataset.openReview);
  }
}

function showAlert(id, type, message) {
  const element = document.getElementById(id);
  element.className = `alert alert-${type} show`;
  element.innerHTML = message;
}

function hideAlert(id) {
  const element = document.getElementById(id);
  element.className = 'alert';
  element.innerHTML = '';
}

function setButtonLoading(button, loading) {
  button.classList.toggle('loading', loading);
  button.disabled = loading;
}

async function withButtonLoading(buttonId, action) {
  const button = document.getElementById(buttonId);
  setButtonLoading(button, true);

  try {
    return await action();
  } finally {
    setButtonLoading(button, false);
  }
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const data = await parseJsonResponse(response);
  return { response, data };
}

async function submitApplication() {
  hideAlert('submit-alert');

  const firstName = value('s-firstName').trim();
  const lastName = value('s-lastName').trim();
  const personalIdCode = value('s-personalIdCode').trim();
  const loanAmount = parseFloat(value('s-loanAmount'));
  const loanPeriodMonths = parseInt(value('s-loanPeriodMonths'), 10);
  const interestMargin = parseFloat(value('s-interestMargin'));
  const baseInterestRateRaw = value('s-baseInterestRate').trim();
  const hasBaseInterestRate = baseInterestRateRaw !== '';
  const baseInterestRate = hasBaseInterestRate ? parseFloat(baseInterestRateRaw) : null;

  if (!firstName || !lastName || !personalIdCode) {
    showAlert('submit-alert', 'error', 'Please fill in first name, last name, and personal ID code.');
    return;
  }

  if (Number.isNaN(loanAmount) || loanAmount < 5000) {
    showAlert('submit-alert', 'error', 'Loan amount must be at least €5,000.');
    return;
  }

  if (Number.isNaN(loanPeriodMonths) || loanPeriodMonths < 6 || loanPeriodMonths > 360) {
    showAlert('submit-alert', 'error', 'Loan period must be between 6 and 360 months.');
    return;
  }

  if (Number.isNaN(interestMargin) || interestMargin < 0) {
    showAlert('submit-alert', 'error', 'Interest margin must be 0 or higher.');
    return;
  }

  if (hasBaseInterestRate && (Number.isNaN(baseInterestRate) || baseInterestRate < 0)) {
    showAlert('submit-alert', 'error', 'Base interest rate must be 0 or higher.');
    return;
  }

  await withButtonLoading('btn-submit', async () => {
    const payload = {
      firstName,
      lastName,
      personalIdCode,
      loanAmount,
      loanPeriodMonths,
      interestMargin
    };

    if (hasBaseInterestRate) {
      payload.baseInterestRate = baseInterestRate;
    }

    const { response, data } = await requestJson(API, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      showAlert('submit-alert', 'error', formatApiError(data));
      return;
    }

    const id = data.applicationId || data.id;
    if (!id) {
      showAlert('submit-alert', 'error', 'Server response did not include an application ID.');
      return;
    }

    showAlert(
      'submit-alert',
      'success',
      `Application submitted. ID: <strong>${id}</strong> <button type="button" class="inline-link" data-open-review="${id}">Open in Review</button>`
    );
    clearSubmitForm();
  }).catch(() => {
    showAlert('submit-alert', 'error', `Network error. Is the backend running at ${API}?`);
  });
}

function clearSubmitForm() {
  ['s-firstName', 's-lastName', 's-personalIdCode', 's-loanAmount', 's-loanPeriodMonths', 's-interestMargin', 's-baseInterestRate']
    .forEach((id) => {
      document.getElementById(id).value = '';
    });
}

function openReview(id) {
  if (!id || id === 'undefined') {
    showAlert('submit-alert', 'error', 'Cannot open review because the application ID is missing.');
    return;
  }

  switchTab('review');
  document.getElementById('r-id').value = id;
  loadApplication();
}

async function loadApplication() {
  const id = value('r-id').trim();

  if (!id) {
    showAlert('review-alert', 'error', 'Please enter an application ID.');
    return;
  }

  if (!isUuid(id)) {
    showAlert('review-alert', 'error', 'Application ID must be a valid UUID.');
    return;
  }

  hideAlert('review-alert');
  document.getElementById('app-panel').classList.add('hidden');
  document.getElementById('action-row').classList.add('hidden');

  await withButtonLoading('btn-load', async () => {
    const { response, data } = await requestJson(`${API}/${id}`);

    if (!response.ok) {
      showAlert('review-alert', 'error', formatApiError(data));
      return;
    }

    currentAppId = id;
    renderApplication(data);
  }).catch(() => {
    showAlert('review-alert', 'error', 'Network error. Is the backend running?');
  });
}

function renderApplication(data) {
  setText('r-fullname', `${data.firstName} ${data.lastName}`);
  setText('r-idcode', data.personalIdCode || '—');
  setText('r-appid', data.id || '—');
  setText('r-amount', `€ ${formatMoney(data.loanAmount)}`);
  setText('r-period', `${data.loanPeriodMonths} months`);
  setText('r-baserate', `${data.baseInterestRate} %`);
  setText('r-margin', `${data.interestMargin} %`);

  const totalRate = (parseFloat(data.baseInterestRate) + parseFloat(data.interestMargin)).toFixed(3);
  setText('r-totalrate', `${totalRate} %`);
  setText('r-created', data.createdAt ? data.createdAt.slice(0, 16).replace('T', ' ') : '—');
  setText('r-reason', data.rejectionReason || '—');

  const badge = document.getElementById('r-status-badge');
  badge.textContent = data.status || '—';
  badge.className = `badge badge-${data.status}`;

  renderSchedule(data.paymentSchedule || data.paymentScheduleEntries || []);

  const actionRow = document.getElementById('action-row');
  actionRow.classList.toggle('hidden', data.status !== 'IN_REVIEW');

  document.getElementById('app-panel').classList.remove('hidden');
  hideAlert('decision-msg');
}

function renderSchedule(entries) {
  const wrap = document.getElementById('schedule-table-wrap');
  const summary = document.getElementById('schedule-summary');

  if (!entries.length) {
    wrap.innerHTML = '<div class="empty-state"><p>No payment schedule generated yet.</p></div>';
    summary.innerHTML = '';
    return;
  }

  const totalInterest = entries.reduce((sum, entry) => sum + parseFloat(entry.interestAmount), 0);
  const totalPrincipal = entries.reduce((sum, entry) => sum + parseFloat(entry.principalAmount), 0);
  const totalPayment = entries.reduce((sum, entry) => sum + parseFloat(entry.totalPayment), 0);
  const monthlyPayment = parseFloat(entries[0].totalPayment);

  summary.innerHTML = [
    summaryItem('Monthly Payment', `€ ${formatMoney(monthlyPayment)}`),
    summaryItem('Total Principal', `€ ${formatMoney(totalPrincipal)}`),
    summaryItem('Total Interest', `€ ${formatMoney(totalInterest)}`),
    summaryItem('Total Cost', `€ ${formatMoney(totalPayment)}`)
  ].join('');

  const rows = entries.map((entry) => `
    <tr>
      <td>${entry.paymentNumber}</td>
      <td>${entry.paymentDate}</td>
      <td>€ ${formatMoney(entry.principalAmount)}</td>
      <td>€ ${formatMoney(entry.interestAmount)}</td>
      <td>€ ${formatMoney(entry.totalPayment)}</td>
      <td>€ ${formatMoney(entry.remainingBalance)}</td>
    </tr>
  `).join('');

  wrap.innerHTML = `
    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Date</th>
          <th>Principal</th>
          <th>Interest</th>
          <th>Total</th>
          <th>Balance</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
      <tfoot>
        <tr>
          <td>Sum</td>
          <td></td>
          <td>€ ${formatMoney(totalPrincipal)}</td>
          <td>€ ${formatMoney(totalInterest)}</td>
          <td>€ ${formatMoney(totalPayment)}</td>
          <td></td>
        </tr>
      </tfoot>
    </table>
  `;
}

function summaryItem(label, value) {
  return `<div class="summary-item"><div class="s-label">${label}</div><div class="s-value">${value}</div></div>`;
}

async function approveApplication() {
  if (!currentAppId) {
    showAlert('decision-msg', 'error', 'No application is loaded yet.');
    return;
  }

  try {
    const { response, data } = await requestJson(`${API}/${currentAppId}/approve`, { method: 'POST' });

    if (!response.ok) {
      showAlert('decision-msg', 'error', formatApiError(data));
      return;
    }

    renderApplication(data);
    showAlert('decision-msg', 'success', 'Application approved successfully.');
  } catch (error) {
    showAlert('decision-msg', 'error', 'Network error.');
  }
}

function openRejectModal() {
  document.getElementById('reject-modal').classList.add('open');
}

function closeRejectModal() {
  document.getElementById('reject-modal').classList.remove('open');
}

async function confirmReject() {
  if (!currentAppId) {
    showAlert('decision-msg', 'error', 'No application is loaded yet.');
    return;
  }

  const reason = value('reject-reason');
  closeRejectModal();

  try {
    const { response, data } = await requestJson(`${API}/${currentAppId}/reject`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ reason })
    });

    if (!response.ok) {
      showAlert('decision-msg', 'error', formatApiError(data));
      return;
    }

    renderApplication(data);
    showAlert('decision-msg', 'info', `Application rejected with reason: <strong>${reason}</strong>`);
  } catch (error) {
    showAlert('decision-msg', 'error', 'Network error.');
  }
}

function value(id) {
  return document.getElementById(id).value;
}

function setText(id, text) {
  document.getElementById(id).textContent = text;
}

function formatMoney(value) {
  return parseFloat(value).toLocaleString('et-EE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function isUuid(value) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

async function parseJsonResponse(response) {
  const text = await response.text();
  if (!text) {
    return {};
  }

  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function formatApiError(data) {
  if (data?.errors?.length) {
    return data.errors.map((error) => `${error.field}: ${error.message}`).join(' | ');
  }

  return data?.message || data?.error || 'Unknown error from server.';
}
