$(function() {

  var returnUrl = getUrlParams()["returnUrl"];
  var returnHtml = (returnUrl == undefined ? "" :
    "<br/>To return to your previous page, <a href=\"" + returnUrl + "\">click here.<a>");

  var suggestUname = getUrlParams()["suggestedUsername"];
  if (suggestUname != undefined) {
    $('input[name=username]').attr('value', suggestUname);
  }

  var displayMessage = function(htmlMessage) {
    $('.message').html(htmlMessage);
  };

  $('.submit-button').click(function() {

    // collect form data and check for empties
    var inputFields = [ "username", "password", "newPassword", "newPasswordCheck"];
    var formData = {};
    var formValid = true;
    inputFields.forEach(function(inputField) {
      var value = $('input[name=' + inputField + ']').val();
      if (value === undefined || value.trim() === '') {
        formValid = false;
      }
      formData[inputField] = value;
    });

    // display empties message if appropriate
    if (!formValid) {
      displayMessage("All input fields must be non-empty.  Please try again.");
      return;
    }

    // check that passwords match
    if (formData['newPassword'] !== formData['newPasswordCheck']) {
      displayMessage("Your new passwords do not match.  Please try again.");
      return;
    }

    // submit request and handle responses
    $.ajax({
      url: "../changePassword",
      method: "post",
      contentType: "application/json",
      data: JSON.stringify(formData),
      statusCode: {
        200: function() { displayMessage("You have successfully changed your password." + returnHtml); },
        400: function() { displayMessage("A client error occurred.  Please report this bug."); },
        403: function() { displayMessage("Incorrect username or password.  Please try again."); },
        500: function(jqXHR, status, error) {
          displayMessage("A server error occurred (" + status + "/" + error + ").  Please report this bug if the problem persists.");
        }
      }
    });
  });
});
