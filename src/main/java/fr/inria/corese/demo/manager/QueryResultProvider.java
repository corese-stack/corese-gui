package fr.inria.corese.demo.manager;

public interface QueryResultProvider {
    String get_csv(fr.inria.corese.core.kgram.core.Mappings mappings);

    String get_xml(fr.inria.corese.core.kgram.core.Mappings mappings);

    String get_json(fr.inria.corese.core.kgram.core.Mappings mappings);

    String get_tsv(fr.inria.corese.core.kgram.core.Mappings mappings);

    String get_markdown(fr.inria.corese.core.kgram.core.Mappings mappings);

    String get_graph_data(fr.inria.corese.core.kgram.core.Mappings mappings);
}
