
var knownProjects = [
  "eupathdb",
  "amoebadb",
  "cryptodb",
  "fungidb",
  "giardiadb",
  "microsporidiadb",
  "piroplasmadb",
  "plasmodb",
  "toxodb",
  "trichdb",
  "tritrypdb",
  "schistodb",
  "hostdb",
  "orthomcl",
  "clinepidb",
  "microbiomedb",
  "vectorbase"
];

var defaultSiteData = {
  project: 'eupathdb',
  baseUrl: 'https://eupathdb.org/eupathdb',
  cancelUrl: 'https://eupathdb.org/eupathdb'
};

function getWebappName(pathname) {
  var webappName = pathname.substring(1);
  var nextSlashIndex = webappName.indexOf("/");
  if (nextSlashIndex != -1) {
    webappName = webappName.substring(0, nextSlashIndex);
  }
  return webappName;
}

function getReferringSiteData(urlQueryParamName) {

  // figure out site referring user to login page
  var redirectUri = getUrlParams()[urlQueryParamName];
  var baseUrl, cancelUrl, project;
  if (redirectUri == null || redirectUri == '') {
    return defaultSiteData;
  }

  var url = parseURL(redirectUri);
  var webappName = getWebappName(url.pathname);
  var hostParts = url.host.split('.');
  var project = hostParts[hostParts.length - 2];

  if (!knownProjects.includes(project)) {
    return defaultSiteData;
  }

  var baseUrl = url.protocol + "//" + url.host + "/" + webappName;
  var cancelUrl = (url.searchObject.redirectUrl == undefined ?
      baseUrl : decodeURIComponent(url.searchObject.redirectUrl));

  return {
    project: project,
    baseUrl: baseUrl,
    cancelUrl: cancelUrl
  };
}

function addEupathDecorators(siteData) {
  $(".eupathdb-logos" ).load( "logos.html", function() {
    // choose the correct header logo and link
    $('#' + siteData.project + '-logo').css({ display: 'inline' });
    $('.main-logo-container a').attr('href', siteData.baseUrl);
  });
  $(".eupathdb-footer").load("footer.html");
}
