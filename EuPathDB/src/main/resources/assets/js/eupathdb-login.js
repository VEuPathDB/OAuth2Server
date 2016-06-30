$(function() {

  var siteData = getReferringSiteData("redirectUri");
  var project = siteData.project;
  var baseUrl = siteData.baseUrl;
  var cancelUrl = siteData.cancelUrl;

  // load header and footer and choose component logo
  addEupathDecorators(siteData);
  
  // add custom URLs to form
  $('#new-account').attr('href', baseUrl + "/showRegister.do");
  $('#forgot-password').attr('href', baseUrl + "/showResetPassword.do");
  $('.cancel-button').click(function(){ window.location = cancelUrl; });

  // override default messages for various status keys
  var status = getUrlParams()["status"];
  var messages = {
    "failed": "Invalid credentials.  Please try again.",
    "error": "An error occurred during authentication.  Please feel free to try again.",
    "accessdenied": "Your session has expired.<br/>Please revisit <a href=\"" + baseUrl + "\">" + baseUrl + "</a> and try again."
  }
  if (status != undefined) {
    jQuery('.message').html(messages[status]);
  }

});