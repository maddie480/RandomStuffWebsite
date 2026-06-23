document.querySelector("select").addEventListener("change", e => {
    document.location.search =
        "?program=" + document.querySelector("#program") +
        "&language=" + encodeURIComponent(e.target.value);
});