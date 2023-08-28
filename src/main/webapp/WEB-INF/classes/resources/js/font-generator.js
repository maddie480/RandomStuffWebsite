// show or hide "font file" when "font" is changed (show it only if the chosen font is "custom")
function onCustomFontSelect() {
    document.getElementById("fontFileDiv").style = '';
    document.getElementById("fontFile").required = 'required';
    document.getElementById("font-file-name-field").style = '';
    document.getElementById("fontFileName").required = 'required';
}

document.getElementById('font').addEventListener('change', function(e) {
    if (e.target.value === 'custom') {
        onCustomFontSelect();
    } else {
        document.getElementById("fontFileDiv").style = 'display: none';
        document.getElementById("fontFile").removeAttribute('required');
        document.getElementById("font-file-name-field").style = 'display: none';
        document.getElementById("fontFileName").removeAttribute('required');
    }
});

if (document.getElementById('font').value === 'custom') {
    onCustomFontSelect();
}
