
const copyToClipboard = function(str) {
  const el = document.createElement('textarea');
  el.value = str;
  document.body.appendChild(el);
  el.select();
  document.execCommand('copy');
  document.body.removeChild(el);
};

const typeToTag = {
  'discord': '{url}',
  'gitlab': '<img src="{url}" height=32 width=32/>',
  'mattermost': '![alt text]({url})'
}

let choosenType = 'discord';

$('input[name=type]').click(function(object) {
  choosenType = $('input[name=type]:checked').val();
})

$('.target').click(function(object) {
  const url = object.currentTarget.getAttribute('data-target');
  copyToClipboard(typeToTag[choosenType].replace('{url}', url));
});

$('#recherche').keyup(function() {
  const recherche = $('#recherche').val();
  const parrots = $('.target');

  for(let i = 0; i < parrots.length; i++) {
    const parrot = $(parrots[i]);
    if(parrot.attr('title').toLowerCase().includes(recherche.toLowerCase())) {
      parrot.show();
    } else {
      parrot.hide();
    }
  }
});

$('form').submit(function(e) {
  e.preventDefault();
});