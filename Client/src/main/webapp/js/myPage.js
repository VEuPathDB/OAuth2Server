
var oauthServerBase = "http://localhost:8080/oauth/";
var clientId = "apiComponentSite";
var sessionCookieName = "myPageUser";

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

//=================================================
// session cookie maintenance functions
//=================================================

function setSessionCookieValue(obj) {
  setSessionCookieStr(JSON.stringify(obj));
}

function getSessionCookieValue() {
  var value = $.cookie('myPageUser');
  if (value == undefined) return undefined;
  return JSON.parse(value);
}

function removeSessionCookie() {
  $.removeCookie('myPageUser');
}

//=================================================
// functions to interact with OAuth Server
//=================================================

function login(userData) {
  setSessionCookieValue(userData);
  setLoginLink();
}

function applyToken(token) {
  $.ajax({
    method: 'GET',
    url: oauthServerBase + "user",
    headers: { "Authorization": "Bearer " + token },
    dataType: 'json',
    success: function(data) {
      login(data);
    },
    error: function() {
      alert("Unable to get user account information using token: " + token);
    }
  });
}

function applyAuthCode(authCode) {
  $.ajax({
    method: 'POST',
    contentType: 'application/x-www-form-urlencoded',
    url: oauthServerBase + "token",
    data: {
      grant_type: "authorization_code",
      code: authCode,
      redirect_uri: getRawUri(),
      client_id: clientId
    },
    encode: true,
    dataType: 'json',
    success: function(data) {
      // got back token response
      applyToken(data.access_token);
    },
    fail: function() {
      alert("Failed to retrieve token using auth code " + authCode);
    }
  });
}

function logout() {
  // remove local session cookie
  removeSessionCookie();
  // visit oauth server to log out there
  var logoutLink = oauthServerBase + "logout?" + "redirect_uri=" + getRawUri();
  // should be redirected back to this page
  window.location = logoutLink;
}

function getRawUri() {
  return [location.protocol, '//', location.host, location.pathname].join('');
}

function setLoginLink() {
  var user = getSessionCookieValue();
  var loginLink = oauthServerBase + "authorize?" +
    "response_type=code&" +
    "client_id=" + clientId + "&" +
    "redirect_uri=" + getRawUri();
  $('#sessionLink').html(
    (user == undefined) ?
    'Welcome Guest | <a href="' + loginLink + '">Log In</a>' :
    'Welcome ' + user.email + ' | <a href="javascript:logout()">Log Out</a>');
}

function loadPage() {
  var authCodeParam = getUrlParams()["code"];
  // if this is a redirect from the oauth server, then get user info and log in
  if (authCodeParam != undefined) {
    applyAuthCode(authCodeParam);
  }
  // otherwise, just set login link
  setLoginLink();
}

$(loadPage);
