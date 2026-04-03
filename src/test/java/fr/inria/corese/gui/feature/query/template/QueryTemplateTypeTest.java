package fr.inria.corese.gui.feature.query.template;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTemplateTypeTest {

	private static final EnumSet<QueryTemplateType> GRAPH_TYPES = EnumSet.of(QueryTemplateType.SELECT,
			QueryTemplateType.SELECT_COUNT, QueryTemplateType.CONSTRUCT, QueryTemplateType.DESCRIBE,
			QueryTemplateType.ASK, QueryTemplateType.INSERT_DATA, QueryTemplateType.DELETE_DATA,
			QueryTemplateType.DELETE_INSERT_WHERE);
	private static final EnumSet<QueryTemplateType> PATTERN_TYPES = EnumSet.of(QueryTemplateType.SELECT,
			QueryTemplateType.SELECT_COUNT, QueryTemplateType.CONSTRUCT, QueryTemplateType.DESCRIBE,
			QueryTemplateType.ASK);
	private static final EnumSet<QueryTemplateType> DISTINCT_TYPES = EnumSet.of(QueryTemplateType.SELECT);
	private static final EnumSet<QueryTemplateType> ORDER_BY_TYPES = EnumSet.of(QueryTemplateType.SELECT);
	private static final EnumSet<QueryTemplateType> LIMIT_TYPES = EnumSet.of(QueryTemplateType.SELECT,
			QueryTemplateType.CONSTRUCT, QueryTemplateType.DESCRIBE);
	private static final EnumSet<QueryTemplateType> SERVICE_TYPES = EnumSet.of(QueryTemplateType.SELECT,
			QueryTemplateType.SELECT_COUNT, QueryTemplateType.CONSTRUCT, QueryTemplateType.DESCRIBE,
			QueryTemplateType.ASK, QueryTemplateType.DELETE_INSERT_WHERE);

	@Test
	void capabilityMatrix_matchesExpectedTemplateFamilies() {
		for (QueryTemplateType type : QueryTemplateType.values()) {
			assertEquals(GRAPH_TYPES.contains(type), type.supportsGraphClause(), type + " graph support");
			assertEquals(PATTERN_TYPES.contains(type), type.supportsPatternVariant(), type + " pattern support");
			assertEquals(DISTINCT_TYPES.contains(type), type.supportsDistinct(), type + " distinct support");
			assertEquals(ORDER_BY_TYPES.contains(type), type.supportsOrderBy(), type + " order support");
			assertEquals(LIMIT_TYPES.contains(type), type.supportsLimit(), type + " limit support");
			assertEquals(LIMIT_TYPES.contains(type), type.supportsOffset(), type + " offset support");
			assertEquals(SERVICE_TYPES.contains(type), type.supportsServiceClause(), type + " service support");
		}
	}
}
