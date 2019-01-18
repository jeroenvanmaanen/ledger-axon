import REST from 'rest';
var mime = require('rest/interceptor/mime');
var restClient = REST.wrap(mime);

export default restClient;
