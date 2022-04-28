/*
 *  Copyright (c) 2020 Works Applications Co., Ltd.
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

package com.worksap.nlp.elasticsearch.sudachi.index;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.worksap.nlp.elasticsearch.sudachi.plugin.AnalysisSudachiPlugin;
import com.worksap.nlp.lucene.sudachi.ja.ResourceUtil;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.opensearch.Version;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.Index;
import org.opensearch.index.analysis.AnalysisRegistry;
import org.opensearch.index.analysis.TokenizerFactory;
import org.opensearch.indices.analysis.AnalysisModule;
import org.opensearch.test.IndexSettingsModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertTokenStreamContents;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestSudachiAnalysis {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        testFolder.create();
        File sudachiFolder = testFolder.newFolder("config", "sudachi");
        ResourceUtil.copy(sudachiFolder);
        ResourceUtil.copyResource("sudachi.json", sudachiFolder, false);
        ResourceUtil.copyResource("unk.def", sudachiFolder, false);
    }

    @Test
    public void tokenizer() throws IOException {
        Map<String, String> settings = new HashMap<>();
        settings.put("index.analysis.tokenizer.sudachi_tokenizer.type", "sudachi_tokenizer");
        settings.put("index.analysis.tokenizer.sudachi_tokenizer.settings_path", "sudachi/sudachi.json");

        Tokenizer tokenizer = createTestTokenizers(settings).get("sudachi_tokenizer").create();
        tokenizer.setReader(new StringReader("東京へ行く。"));
        assertTerms(tokenizer, "東京", "へ", "行く");
    }

    @Test
    public void tokenizerWithAdditionalSettings() throws IOException {
        String additional;
        try (InputStream is = ResourceUtil.class.getResourceAsStream("additional.json")) {
            additional = ResourceUtil.getSudachiSetting(is);
        }

        Map<String, String> settings = new HashMap<>();
        settings.put("index.analysis.tokenizer.sudachi_tokenizer.type", "sudachi_tokenizer");
        settings.put("index.analysis.tokenizer.sudachi_tokenizer.additional_settings", additional);

        Tokenizer tokenizer = createTestTokenizers(settings).get("sudachi_tokenizer").create();
        tokenizer.setReader(new StringReader("自然言語"));
        assertTerms(tokenizer, "自然", "言語");
    }

    @Test
    public void analyzerProvider() throws IOException {
        Settings indexSettings = Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT).build();
        Settings nodeSettings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), testFolder.getRoot().getPath()).build();
        Environment env = TestEnvironment.newEnvironment(nodeSettings);
        Settings settings = Settings.builder().put("settings_path", "sudachi/sudachi.json").build();
        SudachiAnalyzerProvider provider = new SudachiAnalyzerProvider(IndexSettingsModule.newIndexSettings(new Index("test", "_na_"), indexSettings), env, "sudachi", settings);
        try (TokenStream stream = provider.get().tokenStream("_na_", "東京へ行く。")) {
            assertTokenStreamContents(stream, new String[] { "東京", "行く" });
        }
    }

    Map<String, TokenizerFactory> createTestTokenizers(Map<String, String> settings) throws IOException {
        Settings.Builder builder = Settings.builder();
        settings.forEach((k, v) -> builder.put(k, v));
        builder.put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT);
        Settings indexSettings = builder.build();
        Settings nodeSettings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), testFolder.getRoot().getPath()).build();
        Environment env = TestEnvironment.newEnvironment(nodeSettings);
        AnalysisModule analysisModule = new AnalysisModule(env, Collections.singletonList(new AnalysisSudachiPlugin()));
        AnalysisRegistry analysisRegistry = analysisModule.getAnalysisRegistry();
        return analysisRegistry.buildTokenizerFactories(IndexSettingsModule.newIndexSettings(new Index("test", "_na_"), indexSettings));
    }

    static void assertTerms(TokenStream stream, String... expected) throws IOException {
        stream.reset();
        CharTermAttribute termAttr = stream.getAttribute(CharTermAttribute.class);
        List<String> actual = new ArrayList<>();
        while (stream.incrementToken()) {
            actual.add(termAttr.toString());
        }
        assertThat(actual, is(Arrays.asList(expected)));
    }
}
