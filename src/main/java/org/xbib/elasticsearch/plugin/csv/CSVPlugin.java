package org.xbib.elasticsearch.plugin.csv;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.xbib.elasticsearch.rest.csv.CSVRestSearchAction;

public class CSVPlugin extends AbstractPlugin {

    private final Settings settings;

    @Inject
    public CSVPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "csv";
    }

    @Override
    public String description() {
        return "REST CSV format plugin";
    }


    public void onModule(RestModule module) {
        if (settings.getAsBoolean("plugins.csv.enabled", true)) {
            module.addRestAction(CSVRestSearchAction.class);
        }
    }

}
