/* 公共工具：使用 jQuery 实现轻量的消息提示与抖动动画 */
(function () {
    window.showToast = function (message, type) {
        type = type || 'info';
        var $toast = $('<div class="toast"></div>').addClass('toast-' + type).text(message || '');
        $('body').append($toast);
        requestAnimationFrame(function () {
            $toast.addClass('show');
        });
        setTimeout(function () {
            $toast.removeClass('show');
            setTimeout(function () {
                $toast.remove();
            }, 300);
        }, 2600);
    };

    window.shake = function (selector) {
        var $el = $(selector);
        if (!$el.length) {
            return;
        }
        $el.addClass('shake');
        setTimeout(function () {
            $el.removeClass('shake');
        }, 420);
    };

    window.fmt = function (value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }
        return value;
    };
})();
