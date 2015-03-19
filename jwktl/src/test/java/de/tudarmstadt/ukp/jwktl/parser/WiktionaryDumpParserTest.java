/*******************************************************************************
 * Copyright 2013
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
 ******************************************************************************/
package de.tudarmstadt.ukp.jwktl.parser;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;

import de.tudarmstadt.ukp.jwktl.api.util.Language;
import de.tudarmstadt.ukp.jwktl.parser.util.IDumpInfo;
import junit.framework.TestCase;

/**
 * Test case for {@link WiktionaryDumpParser}.
 */
public class WiktionaryDumpParserTest extends TestCase {

	private static class MyWiktionaryDumpParser extends WiktionaryDumpParser {

		private Queue<String> expectedValues;
		
		protected MyWiktionaryDumpParser(Queue<String> expectedValues) {
			this.expectedValues = expectedValues;
		}

		protected void setBaseURL(String baseURL) {
			assertEquals(expectedValues.poll(), "setBaseURL: " + baseURL);
		}
				
		protected void addNamespace(String namespace) {
			assertEquals(expectedValues.poll(), "addNamespace: " + namespace);
		}

		protected void onPageStart() {
			assertEquals(expectedValues.poll(), "onPageStart");
		}
		
		protected void onPageEnd() {
			assertEquals(expectedValues.poll(), "onPageEnd");
		}

		protected void setPageId(long pageId) {
			assertEquals(expectedValues.poll(), "setPageId: " + pageId);
		}

		protected void setTitle(String title) {
			assertEquals(expectedValues.poll(), "setTitle: " + title);
		}
		
		protected void setAuthor(String author) {
			assertEquals(expectedValues.poll(), "setAuthor: " + author);
		}

		protected void setRevision(long revisionId) {
			assertEquals(expectedValues.poll(), "setRevision: " + revisionId);
		}

		protected void setTimestamp(Date timestamp) {
			final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			assertEquals(expectedValues.poll(), "setTimestamp: "
					+ (timestamp == null ? "null" : dateFormat.format(timestamp)));
		}
		
		protected void setText(String text) {
			assertEquals(expectedValues.poll(), "setText: " + text);
		}
		
	}
	
	/***/
	public void testParsedInformation() {
		Queue<String> expectedValues = new LinkedList<String>();
		expectedValues.offer("setBaseURL: http://de.wiktionary.org/wiki/Wiktionary:Hauptseite");
		expectedValues.offer("addNamespace: Diskussion");
		expectedValues.offer("onPageStart");
			expectedValues.offer("setTitle: Page 1");
			expectedValues.offer("setPageId: 9");
			expectedValues.offer("setRevision: 10763");
			expectedValues.offer("setTimestamp: 2004-09-17T08:23:57Z");
			expectedValues.offer("setAuthor: TJ");
			expectedValues.offer("setText: Text 1");
		expectedValues.offer("onPageEnd");
		expectedValues.offer("onPageStart");
			expectedValues.offer("setTitle: Page 2");
			expectedValues.offer("setPageId: 10");
			expectedValues.offer("setRevision: 10764");
			expectedValues.offer("setTimestamp: 2004-09-17T08:34:29Z");
			expectedValues.offer("setAuthor: TJ");
			expectedValues.offer("setText: Text 2\n\n      Test Test");
		expectedValues.offer("onPageEnd");
				
		MyWiktionaryDumpParser parser = new MyWiktionaryDumpParser(expectedValues);
		parser.parse(new File("src/test/resources/WiktionaryDumpParserTest.xml"));
		assertTrue(expectedValues.isEmpty());
	}
	
	/***/
	public void testParseEmptyFields() {
		Queue<String> expectedValues = new LinkedList<String>();
		expectedValues.offer("onPageStart");
			expectedValues.offer("setTitle: ");
			expectedValues.offer("setPageId: 0");
			expectedValues.offer("setRevision: 0");
			expectedValues.offer("setTimestamp: null");
			expectedValues.offer("setText: ");
		expectedValues.offer("onPageEnd");
		expectedValues.offer("onPageStart");
			expectedValues.offer("setTimestamp: null");
		expectedValues.offer("onPageEnd");
				
		MyWiktionaryDumpParser parser = new MyWiktionaryDumpParser(expectedValues);
		parser.parse(new File("src/test/resources/WiktionaryDumpParserNullTest.xml"));
	}
	
	/***/
	public void testParseTimestamp() throws Exception {
		Calendar expected = new GregorianCalendar(1956, Calendar.MARCH, 17,
				21, 30, 15);
		expected.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		WiktionaryDumpParser parser = new MyWiktionaryDumpParser(null);
		assertEquals(expected.getTime(), 
				parser.parseTimestamp("1956-03-17T21:30:15Z"));
	}

	public void testParseMultistream() throws Exception {
		final List<Long> pageIds = new ArrayList<Long>();
		final IDumpInfo[] siteInfo = new IDumpInfo[1];
		final WiktionaryDumpParser parser = new WiktionaryDumpParser(new EmptyParser() {
			@Override
			public void onSiteInfoComplete(IDumpInfo dumpInfo) {
				if (siteInfo[0] == null) {
					siteInfo[0] = dumpInfo;
				} else {
					throw new IllegalStateException("received onSiteInfoComplete more than once");
				}
			}

			@Override public void setPageId(long pageId) {
				pageIds.add(pageId);
			}
		});

		final File multistreamDump = new File(getClass().getResource("/enwiktionary-20150224-pages-articles-multistream.xml.bz2").getFile());
		final File multistreamDumpIndex = new File(getClass().getResource("/enwiktionary-20150224-pages-articles-multistream-index.txt.bz2").getFile());

		parser.parseMultistream(
				multistreamDump,
				multistreamDumpIndex,
				new MultistreamFilter.IncludingNames("aardvark"));

		assertNotNull("did not parse siteInfo", siteInfo[0]);
		assertEquals(Language.ENGLISH, siteInfo[0].getDumpLanguage());

		assertEquals(100, pageIds.size());
		final long first = pageIds.get(0);
		final long last = pageIds.get(pageIds.size()-1);
		assertEquals(177L, first);
		assertEquals(306L, last);
	}

	static class EmptyParser implements  IWiktionaryPageParser {
		@Override
		public void onParserStart(IDumpInfo dumpInfo) {
		}

		@Override
		public void onSiteInfoComplete(IDumpInfo dumpInfo) {
		}

		@Override
		public void onParserEnd(IDumpInfo dumpInfo) {
		}

		@Override
		public void onClose(IDumpInfo dumpInfo) {
		}

		@Override
		public void onPageStart() {
		}

		@Override
		public void onPageEnd() {
		}

		@Override
		public void setAuthor(String author) {
		}

		@Override
		public void setRevision(long revisionId) {
		}

		@Override
		public void setTimestamp(Date timestamp) {
		}

		@Override
		public void setPageId(long pageId) {
		}

		@Override
		public void setTitle(String title, String namespace) {
		}

		@Override
		public void setText(String text) {
		}
	}
}
