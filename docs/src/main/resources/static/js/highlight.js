function enableHighlight() {
  const elm = document.getElementsByTagName('pre');
  for (var i = 0; i < elm.length; i++)
    elm[i].className = elm[i].className + " prettyprint " + " highlight " + " no-border ";
}

function wrapToFigure() {
  var elm = document.getElementsByTagName('pre');
  for (var i = 0; i < elm.length; i++) {
    newHtml = '<figure class="highlight prettyprint"> ' + elm[i].outerHTML + ' </figure>';
    elm[i].outerHTML = newHtml;
  }
}

function init() {
  wrapToFigure();
  enableHighlight();
}
