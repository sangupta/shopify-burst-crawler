/**
 *
 * shopify-burst-crawler: Java Client for burst.shopify.com API
 * Copyright (c) 2017-2019, Sandeep Gupta
 * 
 * https://sangupta.com/projects/shopify-burst-crawler
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.sangupta.shopify.burst.crawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.consume.GenericConsumer;
import com.sangupta.jerry.http.service.HttpService;
import com.sangupta.jerry.http.service.impl.DefaultHttpServiceImpl;
import com.sangupta.jerry.io.AdvancedStringReader;
import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.DateUtils;
import com.sangupta.jerry.util.GsonUtils;

public class BurstSitemapCrawler {

	private static final String MAIN_SITEMAP_FILE = "https://burst.shopify.com/sitemap.xml";

	private static final Logger LOGGER = LoggerFactory.getLogger(BurstSitemapCrawler.class);

	@Inject
	private HttpService httpService;

	public static void main(String[] args) {
		BurstSitemapCrawler impl = new BurstSitemapCrawler();
		
		impl.httpService = new DefaultHttpServiceImpl();
		impl.httpService.setSocketTimeout((int) DateUtils.FIVE_MINUTES);
		
		impl.getCrawledImageFromPage("https://burst.shopify.com/photos/pouring-hot-coffee");
	}

	/**
	 * Return a list of all crawled {@link BurstImage}s. This may take a
	 * lot of time as all URLs reachable via the sitemap shall be crawled
	 * before the results are returned. For streaming results, use the
	 * {@link #crawl(GenericConsumer)} method.
	 * 
	 * @return {@link List} of {@link BurstImage}s collected 
	 */
	public List<BurstImage> crawl() {
		// initialize image array
		final List<BurstImage> images = new ArrayList<>();

		GenericConsumer<BurstImage> collector = new GenericConsumer<BurstImage>() {
			
			@Override
			public boolean consume(BurstImage image) {
				images.add(image);
				return true;
			}
		};
		
		// start crawling
		this.crawl(collector);

		// return images
		return images;
	}
	
	public void crawl(GenericConsumer<BurstImage> collector) {
		// read sitemap file
		List<String> sitemaps = this.readMainSitemapFile();
		if (AssertUtils.isEmpty(sitemaps)) {
			LOGGER.warn("No sitemaps were discovered from Shopfiy burst");
			return;
		}

		// loop over
		Set<String> visited = new HashSet<>();
		Iterator<String> iterator = sitemaps.iterator();
		while(iterator.hasNext()) {
			String sitemap = iterator.next();
			this.doForSitemap(sitemap, sitemaps, visited, collector);
		}
	}

	/**
	 * Do for individual sitemap
	 * 
	 * @param sitemap
	 * @param sitemaps 
	 * @param images
	 */
	private void doForSitemap(String sitemap, List<String> sitemaps, Set<String> visited, GenericConsumer<BurstImage> collector) {
		if(visited.contains(sitemap)) {
			LOGGER.debug("Shopify Burst sitemap XML already visited: {}", sitemap);
			return;
		}
		
		// add to visited
		visited.add(sitemap);
		
		// download xml
		LOGGER.debug("Downloading Shopify Burst sitemap XML: {}", sitemap);

		String xml = this.httpService.getTextResponse(sitemap);
		if (AssertUtils.isEmpty(xml)) {
			LOGGER.debug("No content for shopify burst sitemap: {}", sitemap);
			return;
		}

		LOGGER.debug("Extracting photo urls from xml length: {}", xml.length());
		AdvancedStringReader reader = new AdvancedStringReader(xml);
		do {
			if (!reader.hasNext()) {
				return;
			}

			String url = reader.readBetween("<loc>", "</loc>");
			if (url == null) {
				return;
			}

			// check if its a sitemap
			if(url.endsWith(".xml")) {
				if(!visited.contains(url)) {
					LOGGER.debug("Adding Shopify Burst sitemap XML: {}", url);
					sitemaps.add(url);
				}
			}
			
			// check if its a photo
			if(url.startsWith("https://burst.shopify.com/photos/")) {
				BurstImage crawledImage = this.getCrawledImageFromPage(url);
				if (crawledImage != null) {
					boolean continueCrawling = collector.consume(crawledImage);
					if(!continueCrawling) {
						LOGGER.debug("Collector returned false after collecting image: {}. Further collection stopped.", url);
						return;
					}
				}
			}
			
			// its some other page url, like author or category
			// we can skip it for now
		} while (true);
	}

	/**
	 * Convert the photo URL such as
	 * 'https://burst.shopify.com/photos/pouring-hot-coffee' to a
	 * {@link CrawledImage} instance.
	 * 
	 * @param url the URL to the page
	 * 
	 * @return the {@link CrawledImage} instance
	 */
	private BurstImage getCrawledImageFromPage(String url) {
		LOGGER.debug("Downloading Shopify photo page: {}", url);
		
		// download the HTML for image page
		String html = this.httpService.getTextResponse(url);
		if(AssertUtils.isEmpty(html)) {
			LOGGER.debug("Unable to download photo page url: {}", url);
			return null;
		}
		
		// export image
		final BurstImage image = new BurstImage();		
		final AdvancedStringReader reader = new AdvancedStringReader(html);

		// copy base values
		image.homeUrl = url;
		
		// parse and extract data
		final Document doc = Jsoup.parse(html);
		this.populateFromHTML(image, doc);
		
		// read name, description from json+ld
		reader.reset();
		final String jsonLinkedData = reader.readBetween("<script type=\"application/ld+json\">", "</script>");
		if(AssertUtils.isNotEmpty(jsonLinkedData)) {
			final BurstJsonLinkedData data = GsonUtils.getGson().fromJson(jsonLinkedData, BurstJsonLinkedData.class);
			
			image.url = data.contentUrl.substring(0, data.contentUrl.indexOf('?'));
			image.title = data.name;
			image.description = data.description;
			image.author = data.author;
			image.licenseUrl = data.license;
		}
		
		return image;
	}
	
	private void populateFromHTML(BurstImage image, Document doc) {
		Elements elements = doc.select("main");
        if (elements == null) {
            return;
        }
        
		Element mainNode = elements.first();
        if(mainNode == null) {
            return;
        }
        
		elements = mainNode.select(".photo__meta a");
        if(elements != null && elements.size() > 0) {
            for(int index = 0; index < elements.size(); index++) {
                Element ele = elements.get(index);
                String href = ele.absUrl("href");
                if(AssertUtils.isEmpty(href)) {
                	href = ele.attr("href");
                }
                
        		// populate author url
                if(href.startsWith("https://burst.shopify.com/@")) {
                    image.authorUrl = href;
                    image.author = ele.text();
                    continue;
                }
                
        		// populate license and license url
                if(href.contains("/licenses/")) {
                    image.license = ele.text();
                    continue;
                }

                // populate tags
                image.tags.add(ele.text());
            }
        }		
	}

	/**
	 * Read child sitemap files
	 * 
	 * @return
	 */
	private List<String> readMainSitemapFile() {
		LOGGER.debug("Downloading Shopify Burst main sitemap XML");
		String content = this.httpService.getTextResponse(MAIN_SITEMAP_FILE);
		if (AssertUtils.isEmpty(content)) {
			LOGGER.debug("No content for shopify burst main sitemap");
			return null;
		}

		List<String> maps = new ArrayList<>();
		AdvancedStringReader reader = new AdvancedStringReader(content);
		do {
			if (!reader.hasNext()) {
				return maps;
			}

			String url = reader.readBetween("<loc>", "</loc>");
			if (url == null) {
				return maps;
			}

			maps.add(url);
		} while (true);
	}

}
