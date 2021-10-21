let timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

document.querySelectorAll('.timezone').forEach(elt => elt.innerText = timezone);
document.getElementById('reveal').style.display = 'block';
document.getElementById('text').style.display = 'none';
