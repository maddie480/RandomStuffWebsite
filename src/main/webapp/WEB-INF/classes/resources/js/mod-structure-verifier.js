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
        document.getElementById("assetFolderName").value = '';
        document.getElementById("mapFolderName").value = '';
    }
});

// also handle the initial values (in case of refresh for example)
if (document.getElementById('checkPaths').checked) {
    onPathsCheckEnable();
}

function slice(file, start, end) {
    var slice = file.mozSlice ? file.mozSlice :
                file.webkitSlice ? file.webkitSlice :
                file.slice ? file.slice : () => {};

    return slice.bind(file)(start, end);
}

function uploadChunk(progress, file, chunkId, chunkIndex, chunkSize, start, end) {
    var xhr = new XMLHttpRequest();

    // if we are uploading chunk 8-12 of a 16 MB file, percentGlobal = 50% and percentFactor = 0.25,
    // so the upload will make progress go from 50 to 75%.
    var percentGlobal = 100 * start / file.size;
    var percentFactor = (end - start) / file.size;

    xhr.upload.addEventListener('progress', function(event) {
        // displaying the progress value as text percentage in the "submit" button
        var percentPart = (100 * event.loaded / event.total);
        var percent = Math.min(100, percentGlobal + percentPart * percentFactor);
        progress.value = 'Upload progress: ' + percent.toFixed(0) + '%';
    }, false);

    xhr.addEventListener('readystatechange', function(event) {
        if (event.target.readyState == 0 || (event.target.readyState == 4 && event.target.status !== 200)) {
            // network error or non-200 status => request broke!
            progress.value = 'An error occurred. Click here to try again.';
            progress.removeAttribute('disabled');
            return;
        }

        if (event.target.readyState == 4) {
            if (chunkIndex === null || chunkIndex === 31) {
                // we just sent the last chunk: the response points to where we should get redirected.
                document.location.href = event.target.responseText;
            } else {
                // upload the next chunk! the response contains the chunk ID to use.
                uploadChunk(progress, file, event.target.responseText, chunkIndex + 1, chunkSize, start + chunkSize, end + chunkSize);
            }
        }
    }, false);

    progress.disabled = 'disabled';

    var formData = new FormData(document.getElementById('verify-form'));

    // cut the file, and provide chunk info (if we have it).
    if (chunkIndex !== null) {
        formData.append('chunkIndex', chunkIndex);
        formData.set('zipFile', slice(file, start, end));
    }
    if (chunkId !== null) {
        formData.append('chunkId', chunkId);
    }

    // send the chunk!
    xhr.open('POST', '/celeste/mod-structure-verifier', true);
    xhr.send(formData);
}

// based on: https://stackoverflow.com/a/37237858
function enhanceFormWithUploadProgress(form, progress) {
    // testing browser support. if no support for the required js APIs, the form will just be posted naturally with no progress showing or chunking.
    var xhr = new XMLHttpRequest();
    if (!(xhr && ('upload' in xhr) && ('onprogress' in xhr.upload)) || !window.FormData) {
        return;
    }

    form.addEventListener('submit', function(e) {
        // prevent regular form posting
        e.preventDefault();

        // get the file to send
        var file = document.getElementById('zipFile').files[0];

        if (file.size < 4194304) {
            // no need to chunk files below 4 MB.
            uploadChunk(progress, file, null, null, null, 0, file.size);
        } else {
            // we want 32 chunks. We cut the file in 31 parts, the 32nd one will be the remainder.
            var chunkSize = Math.floor(file.size / 31);
            uploadChunk(progress, file, null, 0, chunkSize, 0, chunkSize);
        }
    });
};

enhanceFormWithUploadProgress(document.getElementById('verify-form'), document.getElementById('submit-button'));

// handle "Copy URL" button
document.getElementById('copyUrl').addEventListener('click', function(e) {
    e.preventDefault();

    var url = 'https://max480-random-stuff.appspot.com/celeste/mod-structure-verifier';
    var assetFolderName = document.getElementById("assetFolderName").value;
    var mapFolderName = document.getElementById("mapFolderName").value;
    if (assetFolderName.length > 0) {
        url += "?assetFolderName=" + encodeURIComponent(assetFolderName);
    }
    if (mapFolderName.length > 0) {
        if (assetFolderName.length > 0) {
            url += "&mapFolderName=" + encodeURIComponent(mapFolderName);
        } else {
            url += "?mapFolderName=" + encodeURIComponent(mapFolderName);
        }
    }

    navigator.clipboard.writeText(url).then(() => {
        e.target.innerHTML = '\u2705 Copied!';
    }).catch(() => {
        e.target.innerHTML = '\u274C Error!';
    }).finally(() => {
        setTimeout(() => e.target.innerHTML = 'Copy URL', 5000)
    });
});
