package org.ansj.elasticsearch.cat;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Table;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestResponseListener;
import org.elasticsearch.rest.action.cat.AbstractCatAction;
import org.nlpcn.commons.lang.util.StringUtil;

/**
 * 分词的cat
 * Created by zhangqinghua on 16/2/2.
 */
public class AnalyzerCatAction extends AbstractCatAction {

    @Inject
    public AnalyzerCatAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET, "/_cat/analyze", this);
        controller.registerHandler(RestRequest.Method.GET, "/_cat/{index}/analyze", this);
    }

    @Override
    protected RestChannelConsumer doCatRequest(RestRequest request, NodeClient client) {
        String[] texts = request.paramAsStringArrayOrEmptyIfAll("text");
        AnalyzeRequest analyzeRequest = new AnalyzeRequest(request.param("index"));
        analyzeRequest.text(texts);
        analyzeRequest.analyzer(request.param("analyzer"));
        analyzeRequest.field(request.param("field"));

        String tokenizer = request.param("tokenizer");
        if (StringUtil.isNotBlank(tokenizer)) {
            analyzeRequest.tokenizer(tokenizer);
        }

        String[] filters = request.paramAsStringArray("token_filters", request.paramAsStringArray("filters", new String[0]));
        for (String filter : filters) {
            analyzeRequest.addTokenFilter(filter);
        }

        filters = request.paramAsStringArray("char_filters", new String[0]);
        for (String filter : filters) {
            analyzeRequest.addCharFilter(filter);
        }

        return channel -> client.admin().indices().analyze(analyzeRequest, new RestResponseListener<AnalyzeResponse>(channel) {
            @Override
            public RestResponse buildResponse(final AnalyzeResponse analyzeResponse) throws Exception {
                return ChineseRestTable.buildResponse(buildTable(analyzeResponse, request), channel);
            }
        });
    }

    @Override
    protected void documentation(StringBuilder stringBuilder) {
        stringBuilder.append("/_cat/analyze\n");
    }

    @Override
    protected Table getTableWithHeader(RestRequest restRequest) {
        final Table table = new Table();
        table.startHeaders();
        table.addCell("term", "alias:t;desc:term;text-align:left");
        table.addCell("start_offset", "alias:s;desc:start_offset;text-align:left");
        table.addCell("end_offset", "alias:e;desc:end_offset;text-align:left");
        table.addCell("position", "alias:p;desc:position;text-align:left");
        table.addCell("type", "alias:t;desc:type;text-align:left");
        table.endHeaders();
        return table;
    }

    private Table buildTable(final AnalyzeResponse analyzeResponse, final RestRequest request) {
        Table t = getTableWithHeader(request);
        for (AnalyzeResponse.AnalyzeToken token : analyzeResponse.getTokens()) {
            t.startRow();
            t.addCell(token.getTerm());
            t.addCell(token.getStartOffset());
            t.addCell(token.getEndOffset());
            t.addCell(token.getPosition());
            t.addCell(token.getType());
            t.endRow();
        }
        return t;
    }
}
