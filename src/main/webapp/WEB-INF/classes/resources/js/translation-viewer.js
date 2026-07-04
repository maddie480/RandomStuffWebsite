document.querySelector("select").addEventListener("change", e => {
    document.location.search =
        "?program=" + document.querySelector("#program").innerText +
        "&language=" + encodeURIComponent(e.target.value);
});