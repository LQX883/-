/* 首页应用逻辑：Vue 3 + Axios */
(function () {
    const { createApp } = Vue;

    const PERM_META = {
        '用户信息查询': { view: 'users', icon: '👤', desc: '按关键字模糊查询用户信息' },
        '借阅记录查询': { view: 'loanSearch', icon: '📋', desc: '关键字 / 超期 / 日期范围查询' },
        '借阅数据统计': { view: 'stats', icon: '📊', desc: '按类型与专业统计借阅次数' },
        '借书': { view: 'borrow', icon: '📖', desc: '选择可借图书进行借阅' },
        '还书': { view: 'return', icon: '🔁', desc: '归还当前未还的图书' },
        '审阅推荐图书': { view: 'review', icon: '✅', desc: '审阅教师提交的图书推荐' },
        '添加书籍': { view: 'addBook', icon: '➕', desc: '新增图书并自动生成副本' },
        '查看所有书籍': { view: 'books', icon: '📚', desc: '浏览与搜索全部图书' },
        '废弃图书': { view: 'books', icon: '🗑️', desc: '副本全部归还后废弃图书' },
        '编辑图书': { view: 'books', icon: '✏️', desc: '修改图书的基础信息' },
        '图书副本编辑': { view: 'books', icon: '🗂️', desc: '新增或删除图书副本' },
        '个人借阅查询': { view: 'myLoans', icon: '🔍', desc: '查看个人全部 / 未还借阅' },
        '推荐图书': { view: 'recommend', icon: '💡', desc: '向图书馆推荐想看的图书' }
    };

    const app = createApp({
        data() {
            return {
                user: { name: '', role: '', major: '', permissions: [] },
                view: 'dashboard',
                loading: false,

                userKeyword: '',
                userList: [],
                userSearched: false,
                userDetail: null,
                showUserModal: false,

                bookKeyword: '',
                bookList: [],
                newBook: { title: '', author: '', type: '', publisher: '', days: 30, price: 0, stock: 1, introduction: '' },
                editBook: {},
                showEditModal: false,
                copyBook: null,
                copies: [],
                showCopyModal: false,

                borrowKeyword: '',
                borrowable: [],
                myLoans: [],
                unreturned: [],

                myLoanMode: 'all',
                myLoanStart: '',
                myLoanEnd: '',
                myLoanList: [],

                loanMode: 'keyword',
                loanKeyword: '',
                loanStart: '',
                loanEnd: '',
                loanList: [],

                stats: { byType: [], byMajor: [] },

                recommendForm: { bookName: '', author: '', publisher: '', price: null },
                myRecommends: [],

                pending: [],
                reviewForm: { id: '', type: '', days: 30, stock: 1, introduction: '' },
                showApproveModal: false,
                rejectForm: { id: '', reason: '' },
                showRejectModal: false
            };
        },
        computed: {
            avatarText() {
                return this.user.name ? this.user.name.slice(0, 1) : '?';
            },
            isOperator() {
                return this.user.role === '操作员';
            },
            viewTitle() {
                const map = {
                    dashboard: '工作台', users: '用户信息查询', books: '图书管理', addBook: '添加书籍',
                    borrow: '借书', return: '还书', myLoans: '个人借阅查询', loanSearch: '借阅记录查询',
                    stats: '借阅数据统计', recommend: '推荐图书', review: '审阅推荐图书'
                };
                return map[this.view] || '工作台';
            },
            menuItems() {
                const seen = new Set();
                const items = [];
                for (const p of this.user.permissions) {
                    const meta = PERM_META[p];
                    if (meta && !seen.has(meta.view)) {
                        seen.add(meta.view);
                        items.push({ label: p, view: meta.view, icon: meta.icon });
                    }
                }
                return items;
            },
            typeOptions() {
                const set = new Set();
                this.bookList.forEach(b => {
                    if (b.type) {
                        set.add(b.type);
                    }
                });
                return Array.from(set);
            },
            maxType() {
                return Math.max(1, ...this.stats.byType.map(i => Number(i.total) || 0));
            },
            maxMajor() {
                return Math.max(1, ...this.stats.byMajor.map(i => Number(i.total) || 0));
            }
        },
        methods: {
            async loadMe() {
                const res = await api.get('/me');
                if (res.code === 200) {
                    this.user = res.data;
                } else if (res.code === 401) {
                    location.href = 'login.html';
                } else {
                    showToast(res.message || '获取用户信息失败', 'error');
                }
            },
            async logout() {
                try {
                    await api.post('/logout');
                } catch (e) {
                    /* ignore */
                }
                location.href = 'login.html';
            },
            permView(permission) {
                return PERM_META[permission] ? PERM_META[permission].view : 'dashboard';
            },
            permIcon(permission) {
                return PERM_META[permission] ? PERM_META[permission].icon : '•';
            },
            permDesc(permission) {
                return PERM_META[permission] ? PERM_META[permission].desc : '';
            },
            go(view) {
                this.view = view;
                this.onEnter(view);
            },
            onEnter(view) {
                switch (view) {
                    case 'books':
                    case 'addBook':
                        this.loadBooks();
                        break;
                    case 'borrow':
                        this.loadBorrowable();
                        this.loadMyLoans();
                        break;
                    case 'return':
                        this.loadUnreturned();
                        break;
                    case 'myLoans':
                        this.setMyLoanMode('all');
                        break;
                    case 'stats':
                        this.loadStats();
                        break;
                    case 'recommend':
                        this.loadMyRecommends();
                        break;
                    case 'review':
                        this.loadPending();
                        break;
                    default:
                        break;
                }
            },
            roleBadge(role) {
                if (role === '管理员') return 'badge-purple';
                if (role === '操作员') return 'badge-blue';
                if (role === '教师') return 'badge-green';
                return 'badge-gray';
            },
            barWidth(value, max) {
                const v = Number(value) || 0;
                return Math.round((v / max) * 100) + '%';
            },

            /* ============ 用户查询 ============ */
            async loadUsers() {
                this.loading = true;
                try {
                    const res = await api.get('/users/search', { params: { keyword: this.userKeyword } });
                    if (res.code === 200) {
                        this.userList = res.data;
                        this.userSearched = true;
                    } else {
                        showToast(res.message || '查询失败', 'error');
                    }
                } finally {
                    this.loading = false;
                }
            },
            clearUserSearch() {
                this.userKeyword = '';
                this.userList = [];
                this.userSearched = false;
            },
            async viewUser(id) {
                const res = await api.get('/users/detail', { params: { id: id } });
                if (res.code === 200) {
                    this.userDetail = res.data;
                    this.showUserModal = true;
                } else {
                    showToast(res.message || '查询失败', 'error');
                }
            },

            /* ============ 图书管理 ============ */
            async loadBooks() {
                const res = await api.get('/books', { params: { keyword: this.bookKeyword } });
                if (res.code === 200) {
                    this.bookList = res.data;
                } else {
                    showToast(res.message || '查询图书失败', 'error');
                }
            },
            async addBook() {
                if (!this.newBook.title) {
                    showToast('请输入书名', 'error');
                    return;
                }
                this.loading = true;
                try {
                    const res = await api.post('/books/save', this.newBook);
                    if (res.code === 200) {
                        showToast('新增图书成功', 'success');
                        this.resetNewBook();
                        await this.loadBooks();
                    } else {
                        showToast(res.message || '新增失败', 'error');
                    }
                } finally {
                    this.loading = false;
                }
            },
            resetNewBook() {
                this.newBook = { title: '', author: '', type: '', publisher: '', days: 30, price: 0, stock: 1, introduction: '' };
            },
            openEdit(book) {
                this.editBook = {
                    id: book.id,
                    title: book.title,
                    author: book.author,
                    type: book.type,
                    publisher: book.publisher,
                    days: book.days,
                    price: book.price
                };
                this.showEditModal = true;
            },
            async saveEdit() {
                if (!this.editBook.title) {
                    showToast('请输入书名', 'error');
                    return;
                }
                this.loading = true;
                try {
                    const res = await api.put('/books/save', this.editBook);
                    if (res.code === 200) {
                        showToast('保存成功', 'success');
                        this.showEditModal = false;
                        await this.loadBooks();
                    } else {
                        showToast(res.message || '保存失败', 'error');
                    }
                } finally {
                    this.loading = false;
                }
            },
            async discardBook(book) {
                if (!confirm('确定要废弃《' + book.title + '》吗？')) {
                    return;
                }
                const res = await api.post('/books/discard', { id: book.id });
                if (res.code === 200) {
                    showToast('图书已废弃', 'success');
                    await this.loadBooks();
                } else {
                    showToast(res.message || '废弃失败', 'error');
                }
            },
            async openCopies(book) {
                this.copyBook = book;
                this.showCopyModal = true;
                await this.loadCopies();
            },
            async loadCopies() {
                if (!this.copyBook) {
                    return;
                }
                const res = await api.get('/bookcopies', { params: { bookId: this.copyBook.id } });
                if (res.code === 200) {
                    this.copies = res.data;
                } else {
                    showToast(res.message || '查询副本失败', 'error');
                }
            },
            async addCopy() {
                const res = await api.post('/bookcopies', { bookId: this.copyBook.id });
                if (res.code === 200) {
                    showToast('新增副本成功', 'success');
                    await this.loadCopies();
                    await this.loadBooks();
                    this.syncCopyBook();
                } else {
                    showToast(res.message || '新增副本失败', 'error');
                }
            },
            async deleteCopy(copy) {
                if (!confirm('确定要删除该副本吗？')) {
                    return;
                }
                const res = await api.delete('/bookcopies', { params: { id: copy.id } });
                if (res.code === 200) {
                    showToast('删除副本成功', 'success');
                    await this.loadCopies();
                    await this.loadBooks();
                    this.syncCopyBook();
                } else {
                    showToast(res.message || '删除副本失败', 'error');
                }
            },
            syncCopyBook() {
                if (!this.copyBook) {
                    return;
                }
                const found = this.bookList.find(b => b.id === this.copyBook.id);
                if (found) {
                    this.copyBook = found;
                }
            },

            /* ============ 借书 / 还书 ============ */
            async loadBorrowable() {
                const res = await api.get('/books/borrowable', { params: { keyword: this.borrowKeyword } });
                if (res.code === 200) {
                    this.borrowable = res.data;
                } else {
                    showToast(res.message || '查询可借图书失败', 'error');
                }
            },
            async loadMyLoans() {
                const res = await api.get('/loans/my');
                if (res.code === 200) {
                    this.myLoans = res.data;
                }
            },
            async borrow(book) {
                if (!confirm('确定借阅《' + book.title + '》吗？')) {
                    return;
                }
                const res = await api.post('/borrow', { bookId: book.id });
                if (res.code === 200) {
                    showToast('借书成功，应还日期：' + (res.data.dueDate || ''), 'success');
                    await this.loadBorrowable();
                    await this.loadMyLoans();
                } else {
                    showToast(res.message || '借书失败', 'error');
                }
            },
            async loadUnreturned() {
                const res = await api.get('/loans/my', { params: { onlyUnreturned: true } });
                if (res.code === 200) {
                    this.unreturned = res.data;
                }
            },
            async returnLoan(loan) {
                if (!confirm('确定归还《' + loan.bookTitle + '》吗？')) {
                    return;
                }
                const res = await api.post('/return', { loanId: loan.id });
                if (res.code === 200) {
                    showToast('还书成功', 'success');
                    await this.loadUnreturned();
                } else {
                    showToast(res.message || '还书失败', 'error');
                }
            },

            /* ============ 个人借阅 ============ */
            setMyLoanMode(mode) {
                this.myLoanMode = mode;
                this.loadMyLoanList();
            },
            async loadMyLoanList() {
                const params = {};
                if (this.myLoanMode === 'unreturned') {
                    params.onlyUnreturned = true;
                } else if (this.myLoanMode === 'date') {
                    params.start = this.myLoanStart;
                    params.end = this.myLoanEnd;
                }
                const res = await api.get('/loans/my', { params: params });
                if (res.code === 200) {
                    this.myLoanList = res.data;
                } else {
                    showToast(res.message || '查询失败', 'error');
                }
            },

            /* ============ 管理员借阅查询 ============ */
            async searchLoans() {
                const params = { mode: this.loanMode };
                if (this.loanMode === 'date') {
                    params.start = this.loanStart;
                    params.end = this.loanEnd;
                } else {
                    params.keyword = this.loanKeyword;
                }
                const res = await api.get('/loans/search', { params: params });
                if (res.code === 200) {
                    this.loanList = res.data;
                } else {
                    showToast(res.message || '查询失败', 'error');
                }
            },

            /* ============ 统计 ============ */
            async loadStats() {
                const res = await api.get('/loans/stats');
                if (res.code === 200) {
                    this.stats = res.data;
                } else {
                    showToast(res.message || '统计失败', 'error');
                }
            },

            /* ============ 推荐 ============ */
            async submitRecommend() {
                if (!this.recommendForm.bookName) {
                    showToast('请输入书名', 'error');
                    return;
                }
                this.loading = true;
                try {
                    const res = await api.post('/recommends', this.recommendForm);
                    if (res.code === 200) {
                        showToast(res.message || '推荐已提交', 'success');
                        this.recommendForm = { bookName: '', author: '', publisher: '', price: null };
                        await this.loadMyRecommends();
                    } else {
                        showToast(res.message || '提交失败', 'error');
                    }
                } finally {
                    this.loading = false;
                }
            },
            async loadMyRecommends() {
                const res = await api.get('/recommends');
                if (res.code === 200) {
                    this.myRecommends = res.data;
                }
            },
            recBadge(status) {
                if (status === '已通过') return 'badge-green';
                if (status === '已驳回') return 'badge-red';
                return 'badge-amber';
            },

            /* ============ 审阅 ============ */
            async loadPending() {
                const res = await api.get('/recommends', { params: { scope: 'all', status: '待审核' } });
                if (res.code === 200) {
                    this.pending = res.data;
                } else {
                    showToast(res.message || '查询失败', 'error');
                }
            },
            openApprove(recommend) {
                this.reviewForm = { id: recommend.id, type: '', days: 30, stock: 1, introduction: '' };
                this.showApproveModal = true;
            },
            async confirmApprove() {
                const payload = Object.assign({}, this.reviewForm, { action: 'approve' });
                const res = await api.post('/recommends/review', payload);
                if (res.code === 200) {
                    showToast('已通过并加入图书列表', 'success');
                    this.showApproveModal = false;
                    await this.loadPending();
                } else {
                    showToast(res.message || '操作失败', 'error');
                }
            },
            openReject(recommend) {
                this.rejectForm = { id: recommend.id, reason: '' };
                this.showRejectModal = true;
            },
            async confirmReject() {
                if (!this.rejectForm.reason) {
                    showToast('请填写驳回理由', 'error');
                    return;
                }
                const payload = Object.assign({}, this.rejectForm, { action: 'reject' });
                const res = await api.post('/recommends/review', payload);
                if (res.code === 200) {
                    showToast('已驳回该推荐', 'success');
                    this.showRejectModal = false;
                    await this.loadPending();
                } else {
                    showToast(res.message || '操作失败', 'error');
                }
            }
        },
        mounted() {
            this.loadMe();
        }
    });

    app.component('book-table', {
        props: {
            books: { type: Array, default: () => [] },
            operator: Boolean
        },
        emits: ['edit', 'discard', 'copies'],
        template: `
            <div v-if="books.length === 0" class="empty-state">
                <div class="empty-icon">📚</div>暂无图书
            </div>
            <div v-else class="table-wrap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>书名</th><th>作者</th><th>类型</th><th>出版社</th>
                        <th>期限</th><th>价格</th><th>库存</th><th>可借</th><th>状态</th>
                        <th v-if="operator">操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="b in books" :key="b.id">
                        <td>{{ b.title }}</td>
                        <td>{{ b.author }}</td>
                        <td>{{ b.type }}</td>
                        <td>{{ b.publisher }}</td>
                        <td>{{ b.days }} 天</td>
                        <td>¥{{ b.price }}</td>
                        <td>{{ b.stock }}</td>
                        <td><span class="badge badge-green">{{ b.available }}</span></td>
                        <td>
                            <span class="badge" :class="b.status === '正常' ? 'badge-blue' : 'badge-gray'">
                                {{ b.status }}
                            </span>
                        </td>
                        <td v-if="operator">
                            <button class="btn btn-light btn-xs" @click="$emit('edit', b)">编辑</button>
                            <button class="btn btn-light btn-xs" style="margin-left:4px"
                                    @click="$emit('copies', b)">副本</button>
                            <button class="btn btn-danger btn-xs" style="margin-left:4px"
                                    :disabled="b.status !== '正常'" @click="$emit('discard', b)">废弃</button>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        `
    });

    app.component('loan-table', {
        props: {
            loans: { type: Array, default: () => [] },
            showUser: Boolean,
            action: Boolean,
            actionLabel: { type: String, default: '操作' },
            actionClass: { type: String, default: '' }
        },
        emits: ['action'],
        methods: {
            statusBadge(status) {
                if (status === '已还') return 'badge-green';
                if (status === '超期') return 'badge-red';
                return 'badge-amber';
            }
        },
        template: `
            <div class="table-wrap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th v-if="showUser">用户</th>
                        <th>书名</th><th>作者</th>
                        <th>借书日期</th><th>应还日期</th><th>还书日期</th><th>状态</th>
                        <th v-if="action">操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="l in loans" :key="l.id">
                        <td v-if="showUser">
                            {{ l.userName }}
                            <span class="badge badge-gray">{{ l.userRole }}</span>
                            <br><small style="color:#94a3b8">{{ l.userMajor }}</small>
                        </td>
                        <td>{{ l.bookTitle }}</td>
                        <td>{{ l.bookAuthor }}</td>
                        <td>{{ l.borrowDate }}</td>
                        <td>{{ l.dueDate }}</td>
                        <td>{{ l.returnDate || '-' }}</td>
                        <td><span class="badge" :class="statusBadge(l.status)">{{ l.status }}</span></td>
                        <td v-if="action">
                            <button class="btn btn-xs" :class="actionClass" @click="$emit('action', l)">
                                {{ actionLabel }}
                            </button>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        `
    });

    app.mount('#app');
})();
