package org.xbib.elasticsearch.rest.csv;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.search.RestSearchAction;

import java.util.Set;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

public class CSVRestSearchAction extends BaseRestHandler {

    @Inject
    public CSVRestSearchAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/_search_csv", this);
        controller.registerHandler(POST, "/_search_csv", this);
        controller.registerHandler(GET, "/{index}/_search_csv", this);
        controller.registerHandler(POST, "/{index}/_search_csv", this);
        controller.registerHandler(GET, "/{index}/{type}/_search_csv", this);
        controller.registerHandler(POST, "/{index}/{type}/_search_csv", this);
        controller.registerHandler(GET, "/_search_csv/template", this);
        controller.registerHandler(POST, "/_search_csv/template", this);
        controller.registerHandler(GET, "/{index}/_search_csv/template", this);
        controller.registerHandler(POST, "/{index}/_search_csv/template", this);
        controller.registerHandler(GET, "/{index}/{type}/_search_csv/template", this);
        controller.registerHandler(POST, "/{index}/{type}/_search_csv/template", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        SearchRequest searchRequest = RestSearchAction.parseSearchRequest(request);
        searchRequest.listenerThreaded(false);
        String[] s = request.paramAsStringArray("keys", null);
        Set<String> keys = s == null ? null : Sets.newHashSet(s);
        client.search(searchRequest, new CSVToXContentListener(channel, keys));
    }

}
