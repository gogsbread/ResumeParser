// CrowdFlower uses MooTools JS library
$$('.gate-snippet').addEvent('change:relay(input)', function(e, target) {
  // when a checkbox inside an annotation snippet is checked or unchecked, toggle
  // the "selected" class on its containing label to match
  var parent = $$(target).getParents('label')[0];
  parent.toggleClass('selected', target.checked);
});
