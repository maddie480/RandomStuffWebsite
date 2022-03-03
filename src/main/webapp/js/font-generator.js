// show or hide "font file" when "font" is changed (show it only if the chosen font is "custom")
function onCustomFontSelect() {
    document.getElementById("fontFileDiv").style = '';
    document.getElementById("fontFile").required = 'required';
}

document.getElementById('font').addEventListener('change', function(e) {
    if (e.target.value === 'custom') {
        onCustomFontSelect();
    } else {
        document.getElementById("fontFileDiv").style = 'display: none';
        document.getElementById("fontFile").removeAttribute('required');
    }
});

// show the Mod Structure Verifier plug and grey out the custom font option if and only if BMFont is selected
function onBMFontSelect() {
    document.getElementById("bmfont-info").style = '';
    document.getElementById("font-file-name-field").style = 'display: none';
    document.getElementById("customfont-option").innerHTML = 'Custom Font (use libgdx instead)';
    document.getElementById("customfont-option").disabled = 'disabled';
    document.getElementById("fontFileName").removeAttribute('required');
}

document.getElementById('method').addEventListener('change', function(e) {
    if (e.target.value === 'bmfont') {
        onBMFontSelect();

        if (document.getElementById('font').value === 'custom') {
            document.getElementById('font').value = 'japanese';
            document.getElementById("fontFileDiv").style = 'display: none';
            document.getElementById("fontFile").removeAttribute('required');
        }
    } else {
        document.getElementById("bmfont-info").style = 'display: none';
        document.getElementById("font-file-name-field").style = '';
        document.getElementById("customfont-option").innerHTML = 'Custom Font';
        document.getElementById("customfont-option").removeAttribute('disabled');
        document.getElementById("fontFileName").required = 'required';
    }
});

// also handle the initial values (in case of refresh for example)
if (document.getElementById('font').value === 'custom') {
    onCustomFontSelect();
}
if (document.getElementById('method').value === 'bmfont') {
    onBMFontSelect();
}
