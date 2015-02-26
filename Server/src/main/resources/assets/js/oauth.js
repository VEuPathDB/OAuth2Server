
var Util = (function() {

  // creates an object with the query string params/values as properties
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

  // tells whether window.location.pathname ends with the passed string
  function matchUrl(str) {
    var path= window.location.pathname;
    return path.indexOf(str, path.length - str.length) !== -1;
  }

  return {
    getUrlParams: getUrlParams,
    matchUrl: matchUrl
  };
})();

var Pages = (function() {

  function doLoginPage() {
    var urlParams = getUrlParams();
    var redirectUrl = urlParams["redirectUrl"];
    if (redirectUrl != undefined && redirectUrl != "") {
      $('input[name=redirectUrl]').attr('value', redirectUrl);
    }
    var status = urlParams["status"];
    if (status != undefined && status == "failed") {
      $('.message').html("Invalid credentials.  Please try again.");
    }
  }

  function doSuccessPage() {
    var urlParams = getUrlParams();
    var authCode = urlParams["code"];
    $('#authCode').html(authCode);
  }

  var pageMapping = {
    'login.html': doLoginPage,
    'success.html': doSuccessPage
  };

  function loadPage() {
    for (page in pageMapping) {
      if (Util.matchUrl(page)) {
        return pageMapping[page]();
      }
    }
  }

  return {
    loadPage: loadPage;
  };
})();

$(Pages.loadPage);
