
/********************************************
 *** Bootstrap various pages
 *******************************************/

var globalState = {
  loadingRequests: 0,
  subscriptionMeta: null,
  groupMeta: null
};

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
            loadGroupPicker();
            loadSubscriptionPicker();
            break;
          case "subscription.html":
            if (id)
              loadSubscription(id);
            else
              initNewSubscriptionForm();
            break;
          case "group.html":
            if (id)
              loadGroup(id);
            else
              initloadNewGroupForm();
            break;
          default:
            console.error("Unknown page: " + page);
        }
      }
    },
    error: ajaxErrorHandler
  });
});

function loadSubscriptionPicker(additionalCallback) {
  doGet("/oauth/subscriptions", subs => {
    globalState.subscriptionMeta = subs;
    refreshSubscriptionSelect();
    if (additionalCallback) additionalCallback();
  });
}

function loadGroupPicker() {
  doGet("/oauth/groups?includeUnsubscribedGroups=true", groups => {
    globalState.groupMeta = groups;
    refreshGroupSelect();
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

function visitSubscription(subscriptionId) {
  if (!subscriptionId) subscriptionId = $("#subscriptionPicker")[0].selectedOptions[0].value;
  window.location.href = "/oauth/assets/admin/subscription.html?id=" + subscriptionId;
}

function visitGroup(groupId) {
  if (!groupId) groupId = $("#groupPicker")[0].selectedOptions[0].value;
  window.location.href = "/oauth/assets/admin/group.html?id=" + groupId;
}

function initNewSubscriptionForm() {
  $("#title").html("Add New Subscription");
  $("#mode").val("new");
  useEditPanel();
}

function loadSubscription(id) {
  doGet("/oauth/subscriptions/" + id, sub => {
    $("#title").html("Subscription: " + sub.displayName);
    useDisplayPanel();

    // fill display area
    $("#subscriptionId").html(sub.subscriptionId);
    $("#isActive").html(sub.isActive)
    $("#groups").html(sub.groups.map(group =>
        '<li><a href="/oauth/assets/admin/group.html?id=' + group.groupId + '">' + group.displayName + '(' + group.groupId + ')</a></li>'
    ));

    // fill form
    $("#mode").val("edit");
    $("#cancelButton").show();
    $("#displayNameInput").val(sub.displayName);
    let activeValue = isActive ? "yes" : "no";
    $('#isActiveInput option[value="' + activeValue + '"]').prop('selected', true);
  });
}

function saveSubscription() {
  // gather data
  var isNew = $("#mode").val() == "new";
  var data = {
    "displayName": $("#displayNameInput").val(),
    "isActive": $("#isActiveInput")[0].selectedOptions[0].value == "yes"
  };
  if (isNew) {
    doPost("/oauth/subscriptions", data, response => {
      visitSubscription(response.subscriptionId);
    });
  }
  else {
    var subscriptionId = $("#subscriptionId").html();
    doPost("/oauth/subscriptions/" + subscriptionId, data, () => {
      visitSubscription(subscriptionId);
    });
  }
}

function initloadNewGroupForm() {
  $("#title").html("Add New Group");
  $("#mode").val("new");
  useEditPanel();
  loadSubscriptionPicker();
}

function loadGroup(id) {
  const userArrayToHtml = users => users.map(user => "<li>" + user.userId + ": " + user.name + " (" + user.organization + ")</li>");
  doGet("/oauth/groups/" + id, group => {

    // load subscriptions; once loaded, display this group's subscription name and select it in the drop-down
    loadSubscriptionPicker(() => {
      $('#subscriptionPicker option[value="' + group.subscriptionId + '"]').prop('selected', true);
      let sub = globalState.subscriptionMeta.filter(sub => sub.subscriptionId == group.subscriptionId)[0];
      $("#subscriptionName").html('<a href="/oauth/assets/admin/subscription.html?id=' + sub.subscriptionId + '">' + sub.displayName + "</a> (" + (sub.isActive ? " active" : " inactive") + ")");
    });

    $("#title").html("Group: " + group.displayName);
    useDisplayPanel();

    // fill display area
    $("#groupId").html(group.groupId);
    $("#subscriptionToken").html(group.subscriptionToken);
    $("#leads").html(userArrayToHtml(group.leadUsers));
    $("#members").html(userArrayToHtml(group.members));

      // fill form
    $("#mode").val("edit");
    $("#cancelButton").show();
    $("#displayNameInput").val(group.displayName);
    $("#groupLeadIds").val(group.groupLeadIds.join());
  });
}

function saveGroup() {
  // gather data
  var isNew = $("#mode").val() == "new";
  var data = {
    "subscriptionId": $("#subscriptionPicker")[0].selectedOptions[0].value,
    "displayName": $("#displayNameInput").val(),
    "groupLeadIds": getCleanLeadIdsAsArray().join()
  };
  if (isNew) {
    doPost("/oauth/groups", data, response => {
      visitGroup(response.groupId);
    });
  }
  else {
    var groupId = $("#groupId").html();
    doPost("/oauth/groups/" + groupId, data, () => {
      visitGroup(groupId);
    });
  }
}

function fillGroupNameWithSubscriptionName() {
  $("#displayNameInput").val($("#subscriptionPicker option:selected").text());
}

function getCleanLeadIdsAsArray(str) {
  return $("#groupLeadIds").val().split(",").map(s => s.trim());
}

function checkLeadIds() {
  var getUserDisplayValue = user => user.firstName + " " + user.lastName + " (" + user.organization + "), " + user.email;
  var enteredIds = getCleanLeadIdsAsArray();
  doGet("/oauth/user-names?userIds=" + enteredIds.join(), users => {
    $("#resultOfLeadIdCheck").html(enteredIds.map(id => {
      var idResult = users.filter(u => u.sub == id)[0];
      var userStr = idResult.found ? getUserDisplayValue(idResult) : "Not Found";
      return "<li>" + id + ": " + userStr + "</li>";
    }));
  });
}

/********************************************
 *** Utilities
 *******************************************/

function ajaxErrorHandler(response, status, error) {
  let message = "Error occurred:\nStatus: " + status + "\nError: " + error;
  console.log(message);
  alert(message);
}

function doGet(url, successCallback) {
  showLoading();
  $.ajax(url, {
    cache: false,
    dataType: "json",
    success: successCallback,
    error: ajaxErrorHandler,
    complete: hideLoading
  });
}

function doPost(url, data, successCallback) {
  showLoading();
  $.ajax(url, {
    type: "POST",
    cache: false,
    contentType: "application/json",
    data: JSON.stringify(data),
    success: successCallback,
    error: ajaxErrorHandler,
    complete: hideLoading
  });
}

function showLoading() {
  globalState.loadingRequests++;
  if (globalState.loadingRequests > 0) {
    $("#loading").show();
  }
}

function hideLoading() {
  globalState.loadingRequests--;
  if (globalState.loadingRequests <= 0) {
    globalState.loadingRequests = 0;
    $("#loading").hide();
  }
}

function useEditPanel() {
  $("#edit").show();
  $("#display").hide();
}

function useDisplayPanel() {
  $("#edit").hide();
  $("#display").show();
}
