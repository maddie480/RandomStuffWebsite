{
    const allCheckboxes = document.querySelectorAll('input[type="checkbox"]');
    allCheckboxes.forEach(el => el.addEventListener('change', e => {
        let isOneChecked = false;
        allCheckboxes.forEach(checkbox => isOneChecked = isOneChecked || checkbox.checked);
        document.querySelector('.btn-success').disabled = !isOneChecked;
    }));
}