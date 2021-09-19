// show or hide "font file" when "font" is changed (show it only if the chosen font is "custom")
document.getElementById('font').addEventListener('change', function(e) {
    if (e.target.value === 'custom') {
        document.getElementById("fontFileDiv").style = '';
        document.getElementById("fontFile").required = 'required';
    } else {
        document.getElementById("fontFileDiv").style = 'display: none';
        document.getElementById("fontFile").removeAttribute('required');
    }
});

// also handle the initial value (in case of refresh for example)
if (document.getElementById('font').value === 'custom') {
    document.getElementById("fontFileDiv").style = '';
    document.getElementById("fontFile").required = 'required';
}
