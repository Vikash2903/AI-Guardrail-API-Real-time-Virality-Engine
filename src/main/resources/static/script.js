const API_BASE = '/api';

// State
let posts = [];

// DOM Elements
const postsFeed = document.getElementById('postsFeed');
const postContent = document.getElementById('postContent');
const submitPost = document.getElementById('submitPost');
const seedBtn = document.getElementById('seedBtn');
const refreshBtn = document.getElementById('refreshBtn');

// Initialize
async function init() {
    loadPosts();
}

// Fetch Posts
async function loadPosts() {
    try {
        const response = await fetch(`${API_BASE}/posts`);
        posts = await response.json();
        renderPosts();
    } catch (err) {
        console.error('Failed to load posts:', err);
    }
}

// Render UI
function renderPosts() {
    if (posts.length === 0) {
        postsFeed.innerHTML = '<div class="loader">No neural activity detected. Create a post!</div>';
        return;
    }

    postsFeed.innerHTML = posts.map(post => `
        <div class="post" data-id="${post.id}">
            <div class="post-header">
                <div class="author-info">
                    <div class="avatar">${post.authorType === 'BOT' ? '🤖' : '👤'}</div>
                    <div class="meta">
                        <div class="name">${post.authorType === 'BOT' ? 'GuardBot #' : 'Human User #'}${post.authorId}</div>
                        <span class="author-type ${post.authorType === 'BOT' ? 'bot-tag' : ''}">${post.authorType}</span>
                    </div>
                </div>
                <div class="timestamp">${new Date(post.createdAt).toLocaleTimeString()}</div>
            </div>
            <div class="post-content">${post.content}</div>
            <div class="post-footer">
                <button class="action-btn" onclick="likePost(${post.id})">❤️ Like</button>
                <button class="action-btn" onclick="toggleComments(${post.id})">💬 Comment</button>
            </div>
            <div id="comments-${post.id}" class="comment-section">
                <div class="comment-input-area">
                    <input type="text" placeholder="Add a comment..." id="input-${post.id}">
                    <select id="type-${post.id}">
                        <option value="USER">User</option>
                        <option value="BOT">Bot</option>
                    </select>
                    <button class="btn-primary" onclick="addComment(${post.id})">Send</button>
                </div>
                <div id="replies-${post.id}" class="replies-list">
                    <!-- Replies will load here -->
                </div>
            </div>
        </div>
    `).join('');
}

// Actions
async function createPost() {
    const content = postContent.value;
    const authorType = document.getElementById('postAuthorType').value;
    
    if (!content) return;

    try {
        const res = await fetch(`${API_BASE}/posts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ authorId: 1, authorType, content })
        });
        
        if (res.ok) {
            postContent.value = '';
            loadPosts();
            showToast('Safe post transmitted to cluster', 'success');
        }
    } catch (err) {
        showToast('Neural link failed', 'danger');
    }
}

async function addComment(postId) {
    const input = document.getElementById(`input-${postId}`);
    const type = document.getElementById(`type-${postId}`).value;
    const content = input.value;

    if (!content) return;

    try {
        const res = await fetch(`${API_BASE}/posts/${postId}/comments`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                authorId: 1,
                authorType: type,
                content: content,
                parentDepth: 0
            })
        });

        const data = await res.json();

        if (res.status === 429) {
            showToast(`GUARDRAIL ALERT: ${data.error}`, 'danger');
        } else if (res.ok) {
            input.value = '';
            showToast('Comment processed by gateway', 'success');
        }
    } catch (err) {
        showToast('Transmission error', 'danger');
    }
}

async function likePost(postId) {
    try {
        const res = await fetch(`${API_BASE}/posts/${postId}/like`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ authorId: 1, authorType: 'USER' })
        });
        if (res.ok) {
            showToast('Virality score increased (+20)', 'success');
        }
    } catch (err) {
        showToast('Like failed', 'danger');
    }
}

async function seedData() {
    await fetch(`${API_BASE}/seed`, { method: 'POST' });
    showToast('Core system seeded successfully', 'success');
    loadPosts();
}

function showToast(msg, type) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.style.background = type === 'danger' ? '#ef4444' : '#22c55e';
    toast.innerText = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
}

// Event Listeners
submitPost.addEventListener('click', createPost);
seedBtn.addEventListener('click', seedData);
refreshBtn.addEventListener('click', loadPosts);

init();
