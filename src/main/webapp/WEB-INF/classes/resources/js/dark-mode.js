{
    const updateTheme = function() {
        if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
            document.documentElement.setAttribute('data-bs-theme', 'dark');
            document.querySelectorAll('.btn-outline-dark').forEach(e => e.classList.replace('btn-outline-dark', 'btn-outline-light'));
        } else {
            document.documentElement.setAttribute('data-bs-theme', 'light');
            document.querySelectorAll('.btn-outline-light').forEach(e => e.classList.replace('btn-outline-light', 'btn-outline-dark'));
        }
    }

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', updateTheme);
    window.addEventListener('DOMContentLoaded', updateTheme);
}