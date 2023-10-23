const copyToClipboard = function(str) {
    const el = document.createElement('textarea');
    el.value = str;
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
};

$('.copy').click(function(event) {
    var content = event.target.getAttribute('data-name');
    event.preventDefault();

    copyToClipboard('!choose_bg ' + content);
    $('#success').toast('show');
});

$('.copy-default').click(function(event) {
    event.preventDefault();

    copyToClipboard('!reset_bg');
    $('#success').toast('show');
});

$('.copy-game').click(function(event) {
    var content = event.target.getAttribute('data-name');
    event.preventDefault();

    copyToClipboard('!choose_game_bg ' + content);
    $('#success').toast('show');
});

$('#success').toast({delay: 2000});