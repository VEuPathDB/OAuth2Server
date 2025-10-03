$(function() {

  // bootstrap for various pages
  let pathAray = document.location.pathname.split("/");
  let page = pathArray[pathArray.length - 1];
  let query = document.location.search.substr(1).split("&").filter(p => p.startsWith("id"));
  let id = query.length > 0 ? query[0].substr(3) : undefined;
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
