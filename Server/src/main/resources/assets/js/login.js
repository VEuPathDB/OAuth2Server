$(function loadPage() {
  var status = getUrlParams()["status"];
  var messages = {
    "failed": "Invalid credentials.  Please try again.",
    "accessdenied": "Anonymous logins disabled.<br/>Please register your client application."
  }
  if (status != undefined) {
    jQuery('.message').html(messages[status]);
  }
});
