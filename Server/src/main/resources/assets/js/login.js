
$(function() {
  var urlParams = getUrlParams();
  var redirectUrl = urlParams["redirectUrl"];
  if (redirectUrl != undefined && redirectUrl != "") {
    $('input[name=redirectUrl]').attr('value', redirectUrl);
  }
  var status = urlParams["status"];
  if (status != undefined && status == "failed") {
    $('.message').html("Invalid credentials.  Please try again.");
  }
});

function getUrlParams() {
  var urlParams = {};
  var match,
      pl     = /\+/g,  // Regex for replacing addition symbol with a space
      search = /([^&=]+)=?([^&]*)/g,
      decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
      query  = window.location.search.substring(1);
  while (match = search.exec(query)) {
    urlParams[decode(match[1])] = decode(match[2]);
  }
  return urlParams;
}
