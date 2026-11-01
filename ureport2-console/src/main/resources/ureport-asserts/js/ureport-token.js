/*
 * yudao-ureport2: token / tenantId 透明传递（独立 ES5 脚本，不经过 webpack）
 *
 * 1. 只读取当前页 URL query 中的 token、tenantId；两者都为空时不安装任何钩子。
 * 2. 只处理 UReport 自身的同源运行时路径（window._server 对应路径及其子路径），
 *    排除 /res/ 静态资源，拒绝 path 中的 %2f、%5c、%25；外站、外部协议、宿主其它路径一律原样不动。
 * 3. 目标 URL 已有同名参数时不重复追加；追加值使用 encodeURIComponent；已有 query 不重新编码。
 *    校验通过的目标一律返回绝对 URL，避免页面 base 标签把相对 URL 解析到别的 origin。
 * 4. 任何异常都原样返回目标 URL，宁可请求被宿主拒绝，也不把凭据带到别处。
 *
 * 覆盖的发起方式：XMLHttpRequest.open、window.open、iframe.src 赋值、
 * GET/POST 表单提交（submit 事件与原生 form.submit()）、a[href]（现有与新增链接、点击、中键、右键、拖动）。
 */
(function (window, document) {
    'use strict';

    var NAMES = ['token', 'tenantId'];
    // 库为 GET 表单新增的 hidden 字段标记
    var MARK = 'data-ureport-token';
    // 编码斜线/反斜线、双重编码：代理或容器再次解码后可能绕出 UReport 路径
    var BAD_PATH = /%(2f|5c|25)/i;

    function safeDecode(s) {
        try {
            return decodeURIComponent(s.replace(/\+/g, ' '));
        } catch (e) {
            return null;
        }
    }

    function pairName(pair) {
        var eq = pair.indexOf('=');
        var raw = eq < 0 ? pair : pair.substring(0, eq);
        var decoded = safeDecode(raw);
        return decoded === null ? raw : decoded;
    }

    // 与 Servlet getParameter 一致：取第一次出现的值；为空或无法解码时不传递
    function readCurrent() {
        var result = [];
        var search = window.location.search;
        if (!search || search.length < 2) {
            return result;
        }
        var pairs = search.substring(1).split('&');
        for (var n = 0; n < NAMES.length; n++) {
            for (var i = 0; i < pairs.length; i++) {
                var pair = pairs[i];
                if (pair === '' || pairName(pair) !== NAMES[n]) {
                    continue;
                }
                var eq = pair.indexOf('=');
                var raw = eq < 0 ? '' : pair.substring(eq + 1);
                var value = raw === '' ? null : safeDecode(raw);
                if (value !== null && value !== '') {
                    result.push({name: NAMES[n], pair: NAMES[n] + '=' + encodeURIComponent(value)});
                }
                break;
            }
        }
        return result;
    }

    var CURRENT = readCurrent();
    if (CURRENT.length === 0 || typeof window.URL !== 'function') {
        return;
    }

    // window._server 在调用时读取（预览页中它定义在本脚本之后）
    function serverBase() {
        var server = window._server;
        if (typeof server !== 'string' || server === '') {
            return null;
        }
        var u = new window.URL(server, window.location.href);
        if (u.origin !== window.location.origin) {
            return null;
        }
        var p = u.pathname;
        while (p.length > 1 && p.charAt(p.length - 1) === '/') {
            p = p.substring(0, p.length - 1);
        }
        return (p === '' || p === '/') ? null : p;
    }

    function hasParam(query, name) {
        if (!query) {
            return false;
        }
        var pairs = query.split('&');
        for (var i = 0; i < pairs.length; i++) {
            if (pairs[i] !== '' && pairName(pairs[i]) === name) {
                return true;
            }
        }
        return false;
    }

    // 目标属于同源 UReport 运行时路径时，返回 {href: 带凭据的绝对 URL, query: 带凭据的原始 query}；否则返回 null
    function resolveInternal(url) {
        if (url === null || url === undefined) {
            return null;
        }
        var str = String(url);
        if (str === '') {
            return null;
        }
        var base = serverBase();
        if (!base) {
            return null;
        }
        var target = new window.URL(str, window.location.href);
        if (target.protocol !== window.location.protocol || target.origin !== window.location.origin) {
            return null;
        }
        var path = target.pathname;
        if (BAD_PATH.test(path)) {
            return null;
        }
        if (path !== base && path.indexOf(base + '/') !== 0) {
            return null;
        }
        if (path.indexOf(base + '/res/') === 0) {
            return null;
        }
        // 序列化后的 href 中，path/query 里的 # 都已编码，第一个 # 即 hash 起点
        var href = target.href;
        var hashIndex = href.indexOf('#');
        var main = hashIndex < 0 ? href : href.substring(0, hashIndex);
        var hash = hashIndex < 0 ? '' : href.substring(hashIndex);
        var qIndex = main.indexOf('?');
        var query = qIndex < 0 ? '' : main.substring(qIndex + 1);
        var extra = '';
        for (var i = 0; i < CURRENT.length; i++) {
            if (!hasParam(query, CURRENT[i].name)) {
                extra += (extra === '' ? '' : '&') + CURRENT[i].pair;
            }
        }
        if (extra !== '') {
            main += (qIndex < 0 ? '?' : (/[?&]$/.test(main) ? '' : '&')) + extra;
            query += (query === '' || /&$/.test(query) ? '' : '&') + extra;
        }
        return {href: main + hash, query: query};
    }

    function withAuth(url) {
        try {
            var parts = resolveInternal(url);
            return parts ? parts.href : url;
        } catch (e) {
            return url;
        }
    }

    var slice = Array.prototype.slice;

    // XMLHttpRequest：覆盖 jQuery $.ajax / $.get 等全部 XHR
    var xhrProto = window.XMLHttpRequest && window.XMLHttpRequest.prototype;
    if (xhrProto && typeof xhrProto.open === 'function') {
        var originalXhrOpen = xhrProto.open;
        xhrProto.open = function () {
            var args = slice.call(arguments);
            if (args.length > 1) {
                args[1] = withAuth(args[1]);
            }
            return originalXhrOpen.apply(this, args);
        };
    }

    // window.open：空 URL（空白窗口）保持原行为
    var originalWindowOpen = window.open;
    if (typeof originalWindowOpen === 'function') {
        window.open = function () {
            var args = slice.call(arguments);
            if (args.length > 0 && typeof args[0] === 'string' && args[0] !== '') {
                args[0] = withAuth(args[0]);
            }
            return originalWindowOpen.apply(window, args);
        };
    }

    // iframe.src 赋值（jQuery .prop('src', url) 即走此 setter）
    var iframeProto = window.HTMLIFrameElement && window.HTMLIFrameElement.prototype;
    var srcDescriptor = iframeProto && Object.getOwnPropertyDescriptor(iframeProto, 'src');
    if (srcDescriptor && srcDescriptor.configurable && typeof srcDescriptor.set === 'function') {
        Object.defineProperty(iframeProto, 'src', {
            configurable: true,
            enumerable: srcDescriptor.enumerable,
            get: srcDescriptor.get,
            set: function (value) {
                srcDescriptor.set.call(this, withAuth(value));
            }
        });
    }

    // 表单：先清理库自己加的 hidden，再按当前 action 重新判断，转为外部 action 的表单不会留下凭据
    function clearHidden(form) {
        try {
            var list = form.querySelectorAll('input[' + MARK + ']');
            for (var i = list.length - 1; i >= 0; i--) {
                list[i].parentNode.removeChild(list[i]);
            }
        } catch (e) {
            // 忽略
        }
    }

    // 会随提交发送的已有控件名（用户显式输入优先，不重复）
    function controlNames(form, submitter) {
        var names = Object.create(null);
        var els = form.elements;
        for (var i = 0; i < els.length; i++) {
            var el = els[i];
            var name = el.name;
            if (!name || el.disabled || el.hasAttribute(MARK) || (typeof el.matches === 'function' && el.matches(':disabled'))) {
                continue;
            }
            var type = String(el.type || '').toLowerCase();
            if ((type === 'checkbox' || type === 'radio') && !el.checked) {
                continue;
            }
            if ((type === 'submit' || type === 'image' || type === 'button' || type === 'reset') && el !== submitter) {
                continue;
            }
            if (type === 'fieldset' || type === 'output' || el.nodeName.toLowerCase() === 'object') {
                continue;
            }
            names[name] = true;
        }
        return names;
    }

    // GET 提交时浏览器会丢弃 action 的 query，把已校验目标的 query 写成 hidden 字段
    function addHidden(form, submitter, query) {
        if (!query) {
            return;
        }
        var taken = controlNames(form, submitter);
        var pairs = query.split('&');
        for (var i = 0; i < pairs.length; i++) {
            var pair = pairs[i];
            if (pair === '') {
                continue;
            }
            var eq = pair.indexOf('=');
            var name = safeDecode(eq < 0 ? pair : pair.substring(0, eq));
            // 只解码一次，浏览器提交时再编码一次，原有双重编码（如 _u）保持不变
            var value = eq < 0 ? '' : safeDecode(pair.substring(eq + 1));
            if (name === null || name === '' || value === null || taken[name] === true) {
                continue;
            }
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = value;
            input.setAttribute(MARK, '1');
            form.appendChild(input);
        }
    }

    function fixForm(form, submitter) {
        try {
            if (!form || typeof form.getAttribute !== 'function') {
                return;
            }
            clearHidden(form);
            if (!submitter || typeof submitter.hasAttribute !== 'function') {
                submitter = null;
            }
            var owner = form, attr = 'action';
            if (submitter && submitter.hasAttribute('formaction')) {
                owner = submitter;
                attr = 'formaction';
            }
            // 读属性而非 form.method，避免被名为 method 的控件遮蔽；无效值按浏览器规则视为 get
            var method = submitter && submitter.hasAttribute('formmethod') ? submitter.getAttribute('formmethod') : form.getAttribute('method');
            method = String(method || 'get').toLowerCase();
            if (method === 'dialog') {
                return;
            }
            if (method !== 'post') {
                method = 'get';
            }
            // action 为空时浏览器提交到当前文档 URL
            var action = owner.getAttribute(attr);
            var parts = resolveInternal(action === null || action === '' ? window.location.href : action);
            if (!parts) {
                return;
            }
            if (action !== parts.href) {
                owner.setAttribute(attr, parts.href);
            }
            if (method === 'get') {
                addHidden(form, submitter, parts.query);
            }
        } catch (e) {
            clearHidden(form);
        }
    }

    function onSubmit(event) {
        var form = event.target;
        fixForm(form, event.submitter);
        // 提交数据在 submit 事件之后同步生成，随后清理，避免 hidden 残留被其它代码读取
        window.setTimeout(function () {
            clearHidden(form);
        }, 0);
    }

    document.addEventListener('submit', onSubmit, true);
    // 冒泡末端再判断一次：表单自身的 submit 处理器若把 action 改到外部，这里会清掉库加的 hidden
    window.addEventListener('submit', onSubmit, false);
    var formProto = window.HTMLFormElement && window.HTMLFormElement.prototype;
    if (formProto && typeof formProto.submit === 'function') {
        var originalFormSubmit = formProto.submit;
        // 原生 form.submit() 不触发 submit 事件
        formProto.submit = function () {
            fixForm(this, null);
            try {
                return originalFormSubmit.apply(this, arguments);
            } finally {
                clearHidden(this);
            }
        };
    }

    // a[href]：改写现有与新增链接，并在交互事件中再确认，覆盖中键、右键和拖动到新标签页
    function isLink(node) {
        return !!node && node.nodeType === 1 && node.nodeName.toLowerCase() === 'a' && node.hasAttribute('href');
    }

    function fixAnchor(link) {
        try {
            var href = link.getAttribute('href');
            if (!href || href.charAt(0) === '#') {
                return;
            }
            var fixed = withAuth(href);
            // 只在字符串实际变化时写回，避免 MutationObserver 自触发循环
            if (fixed !== href) {
                link.setAttribute('href', fixed);
            }
        } catch (e) {
            // 原样跳转
        }
    }

    function scanLinks(root) {
        if (isLink(root)) {
            fixAnchor(root);
        }
        if (typeof root.querySelectorAll !== 'function') {
            return;
        }
        var list = root.querySelectorAll('a[href]');
        for (var i = 0; i < list.length; i++) {
            fixAnchor(list[i]);
        }
    }

    function fixLink(event) {
        var node = event.target;
        while (node && node !== document && !isLink(node)) {
            node = node.parentNode;
        }
        if (isLink(node)) {
            fixAnchor(node);
        }
    }

    var linkEvents = ['mousedown', 'click', 'contextmenu', 'dragstart'];
    for (var i = 0; i < linkEvents.length; i++) {
        document.addEventListener(linkEvents[i], fixLink, true);
    }

    // DOMContentLoaded 时 window._server 已定义
    function watchLinks() {
        scanLinks(document);
        if (typeof window.MutationObserver !== 'function') {
            return;
        }
        new window.MutationObserver(function (records) {
            for (var r = 0; r < records.length; r++) {
                var record = records[r];
                if (record.type === 'attributes') {
                    if (isLink(record.target)) {
                        fixAnchor(record.target);
                    }
                    continue;
                }
                var added = record.addedNodes;
                for (var n = 0; n < added.length; n++) {
                    if (added[n].nodeType === 1) {
                        scanLinks(added[n]);
                    }
                }
            }
        }).observe(document.documentElement, {childList: true, subtree: true, attributes: true, attributeFilter: ['href']});
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', watchLinks);
    } else {
        watchLinks();
    }
})(window, document);
