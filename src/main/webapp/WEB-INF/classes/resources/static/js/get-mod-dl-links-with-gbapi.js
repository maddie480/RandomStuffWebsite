const inp = document.querySelector('input[type="text"]');
const btn = document.querySelector('input[type="button"]');
const sct = document.querySelector('select');

if (! window.jQuery) {
    log("jQuery has failed to load. Please make sure jquery-3.6.0.min.js is present.");
    btn.disabled = true;
}

btn.addEventListener("click", main);

function getModID() {
    return String(inp.value);
}

function getModType() {
    return String(sct.value);
}

function isModIDNotValid() {
    if (! /^[0-9]{1,7}$/.test(getModID())) {
        return true;
    }
}

function main() {
    if (isModIDNotValid()) {
        log("Invalid Mod ID specified - " + getModID());
        return;
    }

    let x = getModID();
    let y = getModType();
    log("Requesting " + y + "/" + x);

    btn.disabled = true;

    $.get( "https://gamebanana.com/apiv5/" + y + "/" + x + "?_csvProperties=_aFiles" , function(data) {
        if (data._aFiles) {
            for (let item of data._aFiles) {
                log(item._sFile, item._sDownloadUrl);
            }

            btn.disabled = false;
        } else {
            log("Nothing found.");
            btn.disabled = false;
        }
    });
}

function log(msg, link) {
    let x = document.querySelector("div");
    let d = new Date();

    x.appendChild(document.createTextNode("[" + d.toISOString() + "] "));
    if (link !== undefined) {
        let l = document.createElement("a");
        l.setAttribute('href', link);
        l.setAttribute('target', '_blank');
        l.innerText = link;
        x.appendChild(l);
    }

    x.appendChild(document.createTextNode(' ' + msg));

    x.appendChild(document.createElement("br"));
    x.scrollTop = x.scrollHeight;
}

$(document).ajaxError(function () {
    log("There was an error requesting given Mod ID :( See Console for info.");
    btn.disabled = false;
});
