let currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;

// Показ/скрытие секций
function showSection(sectionId) {
    if (document.querySelector('.section')) {
        document.querySelectorAll('.section').forEach(section => {
            section.classList.remove('active');
        });
        const targetSection = document.getElementById(sectionId);
        if (targetSection) {
            targetSection.classList.add('active');
        }
    }
}

// Показ модального окна
function showModal(message) {
    const modal = document.getElementById('modal');
    const modalMessage = document.getElementById('modal-message');
    if (modal && modalMessage) {
        modalMessage.textContent = message;
        modal.style.display = 'flex';

        // Закрытие модального окна
        document.querySelector('.modal-close').onclick = () => {
            modal.style.display = 'none';
        };

        // Закрытие при клике вне модального окна
        modal.onclick = (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        };
    }
}

// Регистрация
async function handleRegister() {
    const username = document.getElementById('reg-username').value;
    const password = document.getElementById('reg-password').value;

    const response = await fetch('/api/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}&role=elector`
    });
    const result = await response.json();

    if (response.ok) {
        showModal(result.message);
        showSection('login-section');
    } else {
        showModal(result.error);
    }
}

// Логин
async function handleLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value.trim();
    const isAdmin = document.getElementById('is-admin').checked;

    try {
        const response = await fetch('http://localhost:8080/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
        });
        const result = await response.json();

        if (response.ok) {
            const role = result.role;
            currentUser = { username, isAdmin: role === 'admin' };
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            if (role === 'admin' && isAdmin) {
                window.location.href = 'admin.html';
            } else if (role === 'elector' && !isAdmin) {
                window.location.href = 'user.html';
            } else {
                showModal('Role mismatch: Please check the "Login as Admin" box correctly.');
            }
        } else {
            showModal(result.error);
        }
    } catch (error) {
        showModal('An error occurred during login: ' + error.message);
    }
}

// Выход
function logout() {
    currentUser = null;
    localStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}

// Отображение кандидатов
async function displayCandidates() {
    const votingList = document.getElementById('voting-list');
    if (!votingList) return;

    const response = await fetch('/api/candidates');
    const candidates = await response.json();
    votingList.innerHTML = '';

    candidates.forEach(candidate => {
        const li = document.createElement('li');
        li.innerHTML = `${candidate.name} <button onclick="vote(${candidate.id})">Vote</button>`;
        votingList.appendChild(li);
    });
}

// Голосование
async function vote(candidateId) {
    if (!currentUser) {
        showModal('Please log in to vote.');
        window.location.href = 'index.html';
        return;
    }

    const response = await fetch('/api/vote', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `username=${encodeURIComponent(currentUser.username)}&candidateId=${candidateId}`
    });
    const result = await response.json();

    if (response.ok) {
        showModal(result.message);
        displayCandidates();
    } else {
        showModal(result.error);
    }
}

// Добавление кандидата
async function addCandidate() {
    if (!currentUser || !currentUser.isAdmin) {
        showModal('Only admins can add candidates.');
        window.location.href = 'index.html';
        return;
    }

    const name = document.getElementById('candidate-name').value;
    if (!name) {
        showModal('Please enter a candidate name.');
        return;
    }

    const response = await fetch('/api/add-candidate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `name=${encodeURIComponent(name)}`
    });
    const result = await response.json();

    if (response.ok) {
        showModal(result.message);
        document.getElementById('candidate-name').value = '';
    } else {
        showModal(result.error);
    }
}

// Просмотр результатов
async function viewResults() {
    if (!currentUser || !currentUser.isAdmin) {
        showModal('Only admins can view results.');
        window.location.href = 'index.html';
        return;
    }

    const response = await fetch('/api/results');
    const results = await response.json();
    const resultsList = document.getElementById('results-list');
    resultsList.innerHTML = '';

    results.forEach(result => {
        const li = document.createElement('li');
        li.textContent = `${result.name}: ${result.votes} votes`;
        resultsList.appendChild(li);
    });
}

// Инициализация страницы
if (window.location.pathname.includes('user.html') && currentUser && !currentUser.isAdmin) {
    displayCandidates();
} else if (window.location.pathname.includes('admin.html') && (!currentUser || !currentUser.isAdmin)) {
    window.location.href = 'index.html';
}