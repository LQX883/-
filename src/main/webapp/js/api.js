/* Axios 封装：统一 API 前缀与登录失效处理 */
const api = axios.create({
    baseURL: 'api',
    timeout: 20000
});

api.interceptors.response.use(function (response) {
    const data = response.data;
    if (data && data.code === 401) {
        if (!/login\.html$/.test(location.pathname)) {
            location.href = 'login.html';
        }
    }
    return data;
}, function (error) {
    if (window.showToast) {
        window.showToast('网络请求失败，请稍后重试', 'error');
    }
    return Promise.reject(error);
});
