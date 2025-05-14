// <abbr> tooltips are just fine... unless you don't have a mouse you can use to hover on items.
// Bootstrap tooltips to the rescue!
if (window.matchMedia('(hover: none)').matches) {
    (() => {
        let openTooltip = null;

        document.querySelectorAll('span > abbr').forEach(m => {
            // create an inner span to keep the abbr styling, and move the text inside it
            const span = document.createElement('span');
            span.innerText = m.innerText;
            m.innerHTML = '';
            m.appendChild(span);

            // Bootstrap tooltips are usually triggered by hovering, and trigger: 'click' won't close them
            // if you click away, so we're going for manual triggering instead!
            const tooltip = new bootstrap.Tooltip(span, {trigger: 'manual', title: m.title});
            span.addEventListener('click', e => {
                if (openTooltip === tooltip) {
                    console.log('Hiding tooltip on second click');
                    tooltip.hide();
                    openTooltip = null;
                } else {
                    if (openTooltip !== null) {
                        console.log('Hiding tooltip on click on another tooltip');
                        openTooltip.hide();
                    }
                    console.log('Showing tooltip');
                    tooltip.show();
                    openTooltip = tooltip;
                }
                e.stopPropagation(); // don't call the event listener below
            });
        });

        // set up an extra document-wide event to handle clicking away from a tooltip
        document.addEventListener('click', e => {
            if (openTooltip !== null) {
                console.log('Hiding tooltip on click away')
                openTooltip.hide();
                openTooltip = null;
            }
        });

        console.log('Bootstrap tooltip setup done!');
    })()
} else {
    console.log('Bootstrap tooltips are not needed on this platform.');
}