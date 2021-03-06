/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dkpro.core.performance;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.getType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public final class PerformanceTestUtil
{

    private PerformanceTestUtil()
    {
        // No instances
    }

    public static SummaryStatistics measureWritePerformance(AnalysisEngineDescription aWriterDesc,
            Iterable<JCas> aTestData)
        throws ResourceInitializationException, AnalysisEngineProcessException
    {
        AnalysisEngine writer = createEngine(aWriterDesc);

        SummaryStatistics stats = new SummaryStatistics();
        
        for (JCas jcas : aTestData) {
            long begin = System.currentTimeMillis();
            writer.process(jcas);
            stats.addValue(System.currentTimeMillis() - begin);
        }

        writer.collectionProcessComplete();
        writer.destroy();

        return stats;
    }

    public static SummaryStatistics measureReadPerformance(
            CollectionReaderDescription aReaderDesc, JCas aJCas, int aIterations)
        throws ResourceInitializationException, CollectionException, IOException,
        ResourceConfigurationException
    {
        CollectionReader reader = createReader(aReaderDesc);

        SummaryStatistics stats = new SummaryStatistics();
        
        CAS cas = aJCas.getCas();
        
        for (int i = 0; i < aIterations; i++) {
            long begin = System.currentTimeMillis();
            reader.getNext(cas);
            stats.addValue(System.currentTimeMillis() - begin);
            reader.reconfigure();
            cas.reset();
        }

        reader.close();
        reader.destroy();

        return stats;
    }

    /**
     * Initializes a CAS with random text, tokens, and sentences.
     * 
     * @param aJCas the CAS
     * @param aTextSize the length of the text to be generated.
     * @param aAnnotationCount the number of annotations to be generated.
     * @param aSeed the random seed to allow for repeatable randomness.
     */
    public static void initRandomCas(JCas aJCas, int aTextSize, int aAnnotationCount, long aSeed)
    {
        List<Type> types = new ArrayList<Type>();
        types.add(getType(aJCas, Token.class));
        types.add(getType(aJCas, Sentence.class));
//        Iterator<Type> i = aJCas.getTypeSystem().getTypeIterator();
//        while (i.hasNext()) {
//            Type t = i.next();
//            if (t.isArray() || t.isPrimitive()) {
//                continue;
//            }
//            if (aJCas.getDocumentAnnotationFs().getType().getName().equals(t.getName())) {
//                continue;
//            }
//            types.add(t);
//        }

        // Initialize randomizer
        Random rnd = new Random(aSeed);

        // Shuffle the types
        for (int n = 0; n < 10; n++) {
            Type t = types.remove(rnd.nextInt(types.size()));
            types.add(t);
        }

        // Generate random text
        aJCas.setDocumentText(RandomStringUtils.random(aTextSize));
        
        // Generate random annotations
        CAS cas = aJCas.getCas();
        for (int n = 0; n < aAnnotationCount; n++) {
            Type t = types.get(n % types.size());
            int length = rnd.nextInt(30);
            int begin = rnd.nextInt(aTextSize);
            int end = begin + length;
            if (end > aTextSize) {
                n--; // Skip and extend loop by one
                continue;
            }
            cas.addFsToIndexes(cas.createAnnotation(t, begin, end));
        }
    }

    public static <T> Iterable<T> repeat(final T aObject, final int aCount)
    {
        return new Iterable<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                return new Iterator<T>()
                {
                    private int i = 0;

                    @Override
                    public boolean hasNext()
                    {
                        return i < aCount;
                    }

                    @Override
                    public T next()
                    {
                        i++;
                        return aObject;
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
