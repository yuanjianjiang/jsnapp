function getElement(nr) {
  if (document.layers)
    return document.layers[nr];
  else if (document.all)
    return document.all[nr];
  else if (document.getElementById)
    return document.getElementById(nr);
  return null;
}

function setFocus(nr) {
  getElement(nr).focus();
}

function setDisplay(nr, disp) {
  var elem = getElement(nr);
  if (elem.display)
    elem.display = disp;
  else if (elem.style)
    elem.style.display = disp;
}

function visible(nr) {
  setDisplay(nr, 'block');
}

function invisible(nr) {
  setDisplay(nr, 'none');
}
