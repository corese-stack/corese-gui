package fr.inria.corese.gui.manager;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;

public class CoreseResultProvider implements QueryResultProvider {
  private final QueryManager stateManager;

  public CoreseResultProvider(QueryManager stateManager) {
    this.stateManager = stateManager;
  }

  @Override
  public String get_csv(Mappings mappings) {
    return stateManager.formatMappings(mappings, ResultFormat.format.CSV_FORMAT);
  }

  @Override
  public String get_xml(Mappings mappings) {
    return stateManager.formatMappings(mappings, ResultFormat.format.XML_FORMAT);
  }

  @Override
  public String get_json(Mappings mappings) {
    return stateManager.formatMappings(mappings, ResultFormat.format.JSON_FORMAT);
  }

  @Override
  public String get_tsv(Mappings mappings) {
    return stateManager.formatMappings(mappings, ResultFormat.format.TSV_FORMAT);
  }

  @Override
  public String get_markdown(Mappings mappings) {
    return stateManager.formatMappings(mappings, ResultFormat.format.MARKDOWN_FORMAT);
  }
}
