/**
 * Bootstrap various pages
 */
$(function() {

  // discover URL and id query param if present
  let pathArray = window.location.pathname.split("/");
  let page = pathArray[pathArray.length - 1];
  let query = window.location.search.substr(1).split("&").filter(p => p.startsWith("id"));
  let id = query.length > 0 ? query[0].substr(3) : undefined;

  // check for admin access and redirect to login page if not admin
  $.ajax("/oauth/check-admin", {
    success: (body) => {
      if (body == "no") {
        let host = window.location.hostname;
        window.location.href = "https://" + host + "/oauth/authorize?" +
            "response_type=code&scope=openid email&state=12345&client_id=apiComponentSite&" +
            "redirect_uri=https://" + host + "/oauth/assets/admin/home.html";
      }
      else {
        // set up the dynamic parts of each page
        switch(page) {
          case "home.html":
            loadAdminSelects();
            break;
          case "subscription.html":
            if (id) loadSubscription(id);
            break;
          case "group.html":
            if (id) loadGroup(id);
            break;
          default:
            console.error("Unknown page: " + page);
        }
      }
    },
    error: ajaxErrorHandler
  });
});

var globalState = {
  subscriptionMeta: null,
  groupMeta: null
};

function ajaxErrorHandler(response, status, error) {
  let message = "Error occurred:\nStatus: " + status + "\nError: " + error;
  console.log(message);
  alert(message);
}

function loadAdminSelects() {
  $("#message").html("Loading...");
  let partsLoaded = 0;
  doGet("/oauth/subscriptions", subs => {
    globalState.subscriptionMeta = subs;
    refreshSubscriptionSelect();
    partsLoaded++;
    if (partsLoaded == 2) $("#message").html("");
  });
  doGet("/oauth/groups?includeUnsubscribedGroups=true", groups => {
    globalState.groupMeta = groups;
    refreshGroupSelect();
    partsLoaded++;
    if (partsLoaded == 2) $("#message").html("");
  });
}

function refreshSubscriptionSelect() {
  let showInactiveSubs = $("#inactiveSubscriptions")[0].checked;
  $("#subscriptionPicker").html(globalState.subscriptionMeta
    .filter(sub => showInactiveSubs || sub.isActive)
    .map(sub => '<option value="' + sub.subscriptionId + '">' + sub.displayName + '</option>'));
}

function refreshGroupSelect() {
  let showInactiveGroups = $("#inactiveGroups")[0].checked;
  $("#groupPicker").html(globalState.groupMeta
    .filter(group => showInactiveGroups || group.isActive)
    .map(group => '<option value="' + group.groupId + '">' + group.groupName + '</option>'));
  
}

function visitSubscription() {
  let subscriptionId = $("#subscriptionPicker")[0].selectedOptions[0].value;
  window.location.href = "/oauth/assets/admin/subscription.html?id=" + subscriptionId;
}

function loadSubscription(id) {
  $("#title").html("Subscription " + id);
}

function loadGroup(id) {
  $("#title").html("Group " + id);
}

function visitGroup() {
  let groupId = $("#groupPicker")[0].selectedOptions[0].value;
  window.location.href = "/oauth/assets/admin/group.html?id=" + groupId;
  
}

function doGet(url, successCallback) {
  $.ajax(url,{
    cache: false,
    dataType: "json",
    success: successCallback,
    error: ajaxErrorHandler
  });
}