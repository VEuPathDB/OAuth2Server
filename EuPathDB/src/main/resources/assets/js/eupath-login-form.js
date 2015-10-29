$(function() {

  // figure out component site referring user to login page
  var redirectUri = getUrlParams()["redirectUri"];
  if (redirectUri == '') {
    var baseUrl = 'http://eupathdb.org/eupathdb';
  }
  else {
    var url = parseURL(redirectUri);
    var webappName = getWebappName(url.pathname);
    var baseUrl = url.protocol + "//" + url.host + "/" + webappName;
  }

  // add custom URLs to form
  $('#new-account').attr('href', baseUrl + "/showRegister.do");
  $('#forgot-password').attr('href', baseUrl + "/showResetPassword.do");
  $('.cancel-button').click(function(){ window.location = baseUrl; });

});

function getWebappName(pathname) {
  var webappName = pathname.substring(1);
  var nextSlashIndex = webappName.indexOf("/");
  if (nextSlashIndex != -1) {
    webappName = webappName.substring(0, nextSlashIndex);
  }
  return webappName;
}