function applyTheme(theme) {
  const body = document.body;
  if (!body) {
    return;
  }
  if (theme === 'dark') {
    body.classList.add('theme-dark');
  } else {
    body.classList.remove('theme-dark');
  }

  const toggle = document.getElementById('themeToggle');
  if (toggle) {
    toggle.textContent = theme === 'dark' ? 'Light theme' : 'Dark theme';
  }
}

function loadTheme() {
  try {
    const saved = localStorage.getItem('theme');
    if (saved === 'dark' || saved === 'light') {
      return saved;
    }
  } catch (e) {
  }
  return 'light';
}

function setTheme(theme) {
  try {
    localStorage.setItem('theme', theme);
  } catch (e) {
  }
  applyTheme(theme);
}

function toggleTheme() {
  const current = document.body && document.body.classList.contains('theme-dark') ? 'dark' : 'light';
  setTheme(current === 'dark' ? 'light' : 'dark');
}

document.addEventListener('DOMContentLoaded', () => {
  applyTheme(loadTheme());
});

function startLiveClock(serverEpochMillis) {
  const startLocal = Date.now();

  function tick() {
    const now = new Date(serverEpochMillis + (Date.now() - startLocal));
    const el = document.getElementById('serverTime');
    if (el) {
      el.textContent = now.toLocaleTimeString();
    }
    updateRunningTimers(now);
    requestAnimationFrame(() => {
      setTimeout(tick, 250);
    });
  }

  tick();
}

function updateRunningTimers(serverNowDate) {
  const nodes = document.querySelectorAll('[data-clock-in-epoch]');
  if (!nodes || nodes.length === 0) {
    return;
  }

  const nowMs = serverNowDate.getTime();
  nodes.forEach((node) => {
    const raw = node.getAttribute('data-clock-in-epoch');
    const startMs = raw ? Number(raw) : NaN;
    if (!Number.isFinite(startMs)) {
      return;
    }
    const diffMs = Math.max(0, nowMs - startMs);
    node.textContent = formatDuration(diffMs);
  });
}

function formatDuration(ms) {
  const totalSeconds = Math.floor(ms / 1000);
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}
