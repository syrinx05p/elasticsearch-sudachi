/*
 *  Copyright (c) 2017 Works Applications Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.worksap.nlp.elasticsearch.sudachi.plugin;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.opensearch.index.analysis.AnalyzerProvider;
import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.index.analysis.TokenizerFactory;
import org.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.Plugin;

import com.worksap.nlp.elasticsearch.sudachi.index.SudachiAnalyzerProvider;
import com.worksap.nlp.elasticsearch.sudachi.index.SudachiBaseFormFilterFactory;
import com.worksap.nlp.elasticsearch.sudachi.index.SudachiNormalizedFormFilterFactory;
import com.worksap.nlp.elasticsearch.sudachi.index.SudachiPartOfSpeechFilterFactory;
import com.worksap.nlp.elasticsearch.sudachi.index.SudachiReadingFormFilterFactory;
import com.worksap.nlp.elasticsearch.sudachi.index.SudachiStopTokenFilterFactory;
import com.worksap.nlp.elasticsearch.sudachi.index.SudachiSplitFilterFactory;
import com.worksap.nlp.elasticsearch.sudachi.index.SudachiTokenizerFactory;

public class AnalysisSudachiPlugin extends Plugin implements AnalysisPlugin {
    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("sudachi_baseform", SudachiBaseFormFilterFactory::new);
        extra.put("sudachi_normalizedform", SudachiNormalizedFormFilterFactory::new);
        extra.put("sudachi_part_of_speech",
                SudachiPartOfSpeechFilterFactory::new);
        extra.put("sudachi_readingform", SudachiReadingFormFilterFactory::new);
        extra.put("sudachi_split", SudachiSplitFilterFactory::new);
        extra.put("sudachi_ja_stop", SudachiStopTokenFilterFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("sudachi_tokenizer", SudachiTokenizerFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return singletonMap("sudachi", SudachiAnalyzerProvider::new);
    }
}
