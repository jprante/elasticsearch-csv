package org.xbib.elasticsearch.rest.csv;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.loader.JsonSettingsLoader;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.support.RestResponseListener;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.xbib.elasticsearch.common.csv.CSVGenerator;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.common.collect.Lists.newLinkedList;

public class CSVToXContentListener extends RestResponseListener<SearchResponse> {

    private final static ESLogger logger = ESLoggerFactory.getLogger("csv");

    private final String[] keys;

    private final boolean withIndex;

    private final boolean withType;

    private final boolean withId;

    public CSVToXContentListener(RestChannel channel, String[] keys,
                                 boolean withIndex, boolean withType, boolean withId) {
        super(channel);
        this.keys = keys;
        this.withIndex = withIndex;
        this.withType = withType;
        this.withId = withId;
    }

    @Override
    public final RestResponse buildResponse(SearchResponse response) throws Exception {
        try {
            StringWriter writer = new StringWriter();
            if (response.getHits() != null && response.getHits().getHits() != null && response.getHits().getHits().length > 0) {
                CSVGenerator gen = new CSVGenerator(writer);
                boolean fieldNamesWritten = false;
                JsonSettingsLoader settingsLoader = new JsonSettingsLoader();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String,String> map = hit.sourceRef() != null ?
                            settingsLoader.load(hit.sourceRef().toUtf8()) : new HashMap<String,String>();
                    Map<String,SearchHitField> fields = hit.getFields();
                    if (fields != null) {
                        for (String field : fields.keySet()) {
                            map.put(field, fields.get(field).getValue().toString());
                        }
                    }
                    if (withIndex) {
                        map.put("_index", hit.getIndex());
                    }
                    if (withType) {
                        map.put("_type", hit.getType());
                    }
                    if (withId) {
                        map.put("_id", hit.getId());
                    }
                    List<String> list = newLinkedList();
                    if (keys == null) {
                        list.addAll(map.keySet());
                    } else {
                        Collections.addAll(list, keys);
                    }
                    if (!fieldNamesWritten) {
                        gen.keys(list);
                        gen.writeKeys();
                        fieldNamesWritten = true;
                    }
                    if (keys == null) {
                        for (String key : map.keySet()) {
                            gen.write(map.get(key));
                        }
                    } else {
                        Set<String> keySet = Sets.newHashSet(keys);
                        Set<String> nonexisting = Sets.difference(keySet, map.keySet());
                        for (String key : list) {
                            if (map.keySet().contains(key)) {
                                gen.write(map.get(key));
                            } else if (nonexisting.contains(key)) {
                                gen.write(null);
                            }
                        }
                    }
                    gen.nextRow();
                }
                gen.close();
            }
            return new BytesRestResponse(response.status(), "text/csv", new BytesArray(writer.toString()), true );
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return new BytesRestResponse(channel, RestStatus.INTERNAL_SERVER_ERROR, t);
        }
    }
}
