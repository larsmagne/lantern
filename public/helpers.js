function getRotation(element) {
  // get the computed style object for the element
  var style = window.getComputedStyle(element);
  // this string will be in the form 'matrix(a, b, c, d, tx, ty)'
  var transformString = style['-webkit-transform']
      || style['-moz-transform']
      || style['transform'] ;
  return transformString;
}

function getPosition(e) {
  return [e.getBoundingClientRect().left + document.documentElement.scrollLeft,
	  e.getBoundingClientRect().top + document.documentElement.scrollTop];
}
