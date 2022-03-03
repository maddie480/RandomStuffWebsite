// show or hide path settings if "enable paths check" is enabled / disabled
function onPathsCheckEnable() {
    document.getElementById("path-checking").style = '';
    document.getElementById("assetFolderName").required = 'required';
}

document.getElementById('checkPaths').addEventListener('change', function(e) {
    if (e.target.checked) {
        onPathsCheckEnable();
    } else {
        document.getElementById("path-checking").style = 'display: none';
        document.getElementById("assetFolderName").removeAttribute('required');
    }
});

// also handle the initial values (in case of refresh for example)
if (document.getElementById('checkPaths').checked) {
    onPathsCheckEnable();
}
