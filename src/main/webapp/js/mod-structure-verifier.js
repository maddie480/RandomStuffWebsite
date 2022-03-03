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

// taken and edited from: https://stackoverflow.com/a/37237858
function enhanceFormWithUploadProgress(form, progress) {
    // testing browser support. if no support for the required js APIs, the form will just be posted naturally with no progress showing.
    var xhr = new XMLHttpRequest();
    if (!(xhr && ('upload' in xhr) && ('onprogress' in xhr.upload)) || !window.FormData) {
        return;
    }

    form.addEventListener('submit', function(e) {
        // prevent regular form posting
        e.preventDefault();

        xhr.upload.addEventListener('loadstart', function(event) {
            //initializing the progress indicator (here we're displaying an element that was hidden)
            progress.disabled = 'disabled';
        }, false);

        xhr.upload.addEventListener('progress', function(event) {
            // displaying the progress value as text percentage, may instead update some CSS to show a bar
            var percent = (100 * event.loaded / event.total);
            progress.value = 'Upload progress: ' + percent.toFixed(0) + '%';
        }, false);

        xhr.upload.addEventListener('load', function(event) {
            // this will be displayed while the server is handling the response (all upload data has been transmitted by now)
            progress.value = 'Upload complete, starting task...';
        }, false);

        xhr.addEventListener('readystatechange', function(event) {
            if (event.target.readyState == 4 && event.target.responseURL) {
                // our response here is a redirect, so go to where we got redirected.
                document.location.href = event.target.responseURL;
            }
        }, false);

        // posting the form with the same method and action as specified by the HTML markup
        xhr.open(this.getAttribute('method'), this.getAttribute('action'), true);
        xhr.send(new FormData(this));
    });
};

enhanceFormWithUploadProgress(document.getElementById('verify-form'), document.getElementById('submit-button'));