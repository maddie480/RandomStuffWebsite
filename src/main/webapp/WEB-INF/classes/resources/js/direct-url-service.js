{
    const refresh = function() {
        if (document.getElementById('bundle').checked) {
            document.getElementById('twoclick').checked = false;
            document.getElementById('mirror').checked = false;
            document.getElementById('twoclick').disabled = true;
            document.getElementById('mirror').disabled = true;
        } else {
            document.getElementById('twoclick').disabled = false;
            document.getElementById('mirror').disabled = false;
        }
    }

    document.getElementById('bundle').addEventListener('change', refresh);
    refresh();
}
