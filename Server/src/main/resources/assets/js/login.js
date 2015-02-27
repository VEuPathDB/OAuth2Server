$(function loadPage() {
  var status = getUrlParams()["status"];
  if (status != undefined && status == "failed") {
    jQuery('.message').html("Invalid credentials.  Please try again.");
  }
});
