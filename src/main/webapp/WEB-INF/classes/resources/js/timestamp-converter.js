// reformat the dates with JavaScript: that allows us to show local time to the user, since their browser knows their timezone.

document.querySelectorAll('.timestamp-long').forEach(elt =>
    elt.innerText = new Date(parseInt(elt.attributes['data-timestamp'].value) * 1000).toLocaleDateString('en-US', {
        hour12: true,
        year: "numeric",
        month: "long",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit",
        second: "2-digit",
    }
));

document.querySelectorAll('.timestamp-short').forEach(elt =>
    elt.innerText = new Date(parseInt(elt.attributes['data-timestamp'].value) * 1000).toLocaleDateString(undefined, {
        hour12: false,
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    }
));
