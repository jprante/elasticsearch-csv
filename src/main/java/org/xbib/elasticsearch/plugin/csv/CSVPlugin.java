
package org.xbib.elasticsearch.plugin.csv;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.xbib.elasticsearch.rest.csv.CSVRestSearchAction;

public class CSVPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "csv-"
                + Build.getInstance().getVersion() + "-"
                + Build.getInstance().getShortHash();
    }

    @Override
    public String description() {
        return "REST CSV format plugin";
    }


    public void onModule(RestModule module) {
        module.addRestAction(CSVRestSearchAction.class);
    }

}
