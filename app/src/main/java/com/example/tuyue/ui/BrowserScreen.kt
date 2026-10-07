private const val SHORT_PAGE_FIX_SCRIPT = """
(function () {
    try {
        const html = document.documentElement;
        const body = document.body;

        if (!html || !body) return;

        const viewportHeight = window.innerHeight;

        const bodyHeight =
            body.getBoundingClientRect().height;

        /*
         * 只处理“网页本身比视口矮”的情况。
         * 长网页完全不碰。
         */
        if (bodyHeight + 1 < viewportHeight) {

            html.style.minHeight = '100%';
            body.style.minHeight = '100vh';

            /*
             * 常见 SPA 根节点。
             * 只有它本身接近整个 body 高度时才补高，
             * 避免随便修改网页内部组件。
             */
            const candidates = [
                document.getElementById('app'),
                document.getElementById('root'),
                document.getElementById('__next')
            ].filter(Boolean);

            candidates.forEach(function (element) {

                const rect =
                    element.getBoundingClientRect();

                if (
                    rect.top <= 1 &&
                    rect.height <= viewportHeight
                ) {
                    element.style.minHeight = '100vh';
                }
            });
        }

        window.dispatchEvent(
            new Event('resize')
        );

    } catch (e) {
    }
})();
"""
