'use strict';

const $ = (id) => document.getElementById(id);
const resultEl = $('result');
const strengthEl = $('strength');
const entropyEl = $('entropy');
const crackEl = $('crack');
const meterEl = $('meter');
const copyBtn = $('copy');
const hintEl = $('hint');
const errorEl = $('error');

const tabs = Array.from(document.querySelectorAll('.tab'));
const panels = {
  password: $('panel-password'),
  passphrase: $('panel-passphrase'),
  pin: $('panel-pin'),
};

let hintTimer = null;

function switchTab(mode) {
  tabs.forEach((tab) => {
    const active = tab.dataset.mode === mode;
    tab.classList.toggle('is-active', active);
    tab.setAttribute('aria-selected', String(active));
  });
  Object.entries(panels).forEach(([key, panel]) => {
    panel.hidden = key !== mode;
  });
}

tabs.forEach((tab) => tab.addEventListener('click', () => switchTab(tab.dataset.mode)));

function showError(message) {
  errorEl.textContent = message;
  errorEl.hidden = false;
}

function clearError() {
  errorEl.hidden = true;
}

function hint(text) {
  hintEl.textContent = text;
  clearTimeout(hintTimer);
  hintTimer = setTimeout(() => { hintEl.textContent = ''; }, 2000);
}

function payloadFor(mode, form) {
  const data = Object.fromEntries(new FormData(form).entries());
  if (mode === 'password') {
    return {
      length: Number(data.length),
      lowercase: data.lowercase === 'on',
      uppercase: data.uppercase === 'on',
      digits: data.digits === 'on',
      special: data.special === 'on',
      excludeAmbiguous: data.excludeAmbiguous === 'on',
    };
  }
  if (mode === 'passphrase') {
    return {
      wordCount: Number(data.wordCount),
      separator: data.separator,
      capitalize: data.capitalize === 'on',
      addDigit: data.addDigit === 'on',
    };
  }
  return {
    length: Number(data.length),
    noLeadingZero: data.noLeadingZero === 'on',
  };
}

function render(response) {
  resultEl.textContent = response.result;
  strengthEl.textContent = 'Стойкость: ' + response.strength;
  entropyEl.textContent = 'Энтропия: ' + response.entropyBits.toFixed(1) + ' бит';
  crackEl.textContent = 'Перебор: ' + response.crackTime;
  meterEl.style.width = Math.min(100, Math.round(response.entropyBits)) + '%';
  resultEl.classList.remove('is-weak', 'is-medium', 'is-strong', 'is-very-strong');
  resultEl.classList.add(strengthClass(response.strength));
  copyBtn.disabled = false;
  clearError();
}

function strengthClass(label) {
  if (label === 'слабый') return 'is-weak';
  if (label === 'средний') return 'is-medium';
  if (label === 'сильный') return 'is-strong';
  return 'is-very-strong';
}

async function generate(mode, form) {
  const submit = form.querySelector('button[type="submit"]');
  submit.disabled = true;
  submit.textContent = 'Генерирую…';
  try {
    const response = await fetch('/api/' + mode, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payloadFor(mode, form)),
      cache: 'no-store',
    });
    if (!response.ok) {
      const body = await response.json().catch(() => null);
      showError((body && body.error) ? body.error : 'Ошибка сервера: HTTP ' + response.status);
      return;
    }
    render(await response.json());
  } catch (err) {
    showError('Не удалось связаться с сервером.');
  } finally {
    submit.disabled = false;
    submit.textContent = 'Сгенерировать';
  }
}

Object.entries(panels).forEach(([mode, panel]) => {
  panel.addEventListener('submit', (event) => {
    event.preventDefault();
    generate(mode, panel);
  });
});

function fallbackCopy(text) {
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.style.position = 'fixed';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  let ok = false;
  try {
    ok = document.execCommand('copy');
  } catch (err) {
    ok = false;
  }
  area.remove();
  return ok;
}

copyBtn.addEventListener('click', async () => {
  const text = resultEl.textContent;
  try {
    await navigator.clipboard.writeText(text);
    hint('Скопировано');
  } catch (err) {
    hint(fallbackCopy(text) ? 'Скопировано' : 'Не удалось скопировать');
  }
});
