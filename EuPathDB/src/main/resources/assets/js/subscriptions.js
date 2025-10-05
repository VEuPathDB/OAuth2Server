/**
 * Bootstrap for various pages
 */
$(function() {

  // discover URL and id query param if present
  let pathAray = document.location.pathname.split("/");
  let page = pathArray[pathArray.length - 1];
  let query = document.location.search.substr(1).split("&").filter(p => p.startsWith("id"));
  let id = query.length > 0 ? query[0].substr(3) : undefined;

  // set up the dynamic parts of each page
  switch(page) {
    case "admin.html":
      loadAdminSelects();
      break;
    case "subscription.html":
      if (id) loadSubscription(id);
      else setForNewSubscription();
      break;
    case "group.html":
      if (id) loadGroup(id);
      else setForNewGroup();
      break;
    default:
      console.error("subscriptions.js file loaded for non-standard page");
  }
});

var globalState = {
  subscriptionMeta: null,
  showInactiveSubs: false,
  groupsMeta: null,
  showInactiveGroups: false
};

function loadAdminSelects() {
  $("#message").html("Loading...");
  let partsLoaded = 0;
  doGet("/oauth/subscriptions", subs => {
    subscriptionMeta = subs;
    refreshSubscriptionSelect();
    partsLoaded++;
    if (partsLoaded == 2) $("#message").html("");
  });
  doGet("/oauth/groups?includeUnsubscribedGroups=true", groups => {
    groupsMeta = groups;
    refreshGroupsSelect();
    partsLoaded++;
    if (partsLoaded == 2) $("#message").html("");
  });
}

function refreshSubscriptionSelect() {
  
}

function refreshGroupsSelect() {
  
}

function doGet(url, successCallback) {
  $.ajax(url,{
    cache: false,
    dataType: "json",
    success: successCallback,
    error: (response, status, error) => {
      let message = "Error occurred:\nStatus: " + status + "\nError: " + error;
      console.log(message);
      alert(message);
    }
  });
}