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

function parseURL(url) {
  var parser = document.createElement('a'),
      searchObject = {},
      queries, split, i;
  // Let the browser do the work
  parser.href = url;
  // Convert query string to object
  queries = parser.search.replace(/^\?/, '').split('&');
  for (i = 0; i < queries.length; i++) {
    split = queries[i].split('=');
    searchObject[split[0]] = split[1];
  }
  var pathname = parser.pathname;
  // special case for IE-11
  if (pathname[0] != '/') pathname = '/' + pathname;
  return {
    protocol: parser.protocol,
    host: parser.host,
    hostname: parser.hostname,
    port: parser.port,
    pathname: pathname,
    search: parser.search,
    searchObject: searchObject,
    hash: parser.hash
  };
}
