package fr.inria.corese.demo.manager;

import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.kgram.core.Mappings;

public class CoreseResultProvider implements QueryResultProvider {
    private final QueryManager stateManager;

    public CoreseResultProvider(QueryManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public String get_csv(fr.inria.corese.core.kgram.core.Mappings mappings) {
        return stateManager.formatMappings(mappings, ResultFormat.format.CSV_FORMAT);
    }

    @Override
    public String get_xml(fr.inria.corese.core.kgram.core.Mappings mappings) {
        return stateManager.formatMappings(mappings, ResultFormat.format.XML_FORMAT);
    }

    @Override
    public String get_json(fr.inria.corese.core.kgram.core.Mappings mappings) {
        return stateManager.formatMappings(mappings, ResultFormat.format.JSON_FORMAT);
    }

    @Override
    public String get_tsv(fr.inria.corese.core.kgram.core.Mappings mappings) {
        return stateManager.formatMappings(mappings, ResultFormat.format.TSV_FORMAT);
    }

    @Override
    public String get_markdown(fr.inria.corese.core.kgram.core.Mappings mappings) {
        return stateManager.formatMappings(mappings, ResultFormat.format.MARKDOWN_FORMAT);
    }

    @Override
    public String get_graph_data(fr.inria.corese.core.kgram.core.Mappings mappings) {
        return stateManager.getLastGraphResult();
    }
}
