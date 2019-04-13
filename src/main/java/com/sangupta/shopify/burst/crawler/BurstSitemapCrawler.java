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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.consume.GenericConsumer;
import com.sangupta.jerry.io.AdvancedStringReader;
import com.sangupta.jerry.util.AssertUtils;

/**
 * Simple CLI tool to crawl Shopify Burst image site and provide a list of all
 * images and their details. This crawler uses the website's sitemap file and
 * traverses each image from there.
 * 
 * @author sangupta
 *
 */
public class BurstSitemapCrawler extends AbstractBurstCrawler {

	/**
	 * My internal logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BurstSitemapCrawler.class);

	/**
	 * The main sitemap file as reported by robots.txt
	 */
	private static final String MAIN_SITEMAP_FILE = "https://burst.shopify.com/sitemap.xml";

	/**
	 * Time last image was crawled - used to leave time gaps between
	 * each fetch
	 */
	private volatile long lastCrawled = 0;
	
	/**
	 * Number of images that we have found
	 */
	private int imagesFound = 0;

	/**
	 * Construct an instance of {@link BurstSitemapCrawler} using default
	 * {@link BurstCrawlerOptions}
	 */
	public BurstSitemapCrawler() {
		this(new BurstCrawlerOptions());
	}

	/**
	 * Construct an instance of {@link BurstSitemapCrawler} using provided
	 * {@link BurstCrawlerOptions}
	 * 
	 * @param options the {@link BurstCrawlerOptions} to use
	 */
	public BurstSitemapCrawler(BurstCrawlerOptions options) {
		super(options);
	}

	/**
	 * Crawl using sitemaps, and collect {@link BurstImage}s using a
	 * {@link GenericConsumer} collector.
	 * 
	 * @param collector the {@link GenericConsumer} to use
	 */
	public void crawl(GenericConsumer<BurstImage> collector) {
		LOGGER.info("Starting to crawl Shopify Burst site");

		// reset stats
		this.imagesFound = 0;
		
		// read sitemap file
		List<String> sitemaps = this.readMainSitemapFile();
		if (AssertUtils.isEmpty(sitemaps)) {
			LOGGER.warn("No sitemaps were discovered from Shopfiy burst");
			return;
		}

		// loop over
		LOGGER.info("Total number of child sitemaps found: {}", sitemaps.size());
		Set<String> visited = new HashSet<>();
		Iterator<String> iterator = sitemaps.iterator();
		while (iterator.hasNext()) {
			String sitemap = iterator.next();
			this.doForSitemap(sitemap, sitemaps, visited, collector);
		}
		
		LOGGER.info("Shopify Burst site crawling completed");
	}

	/**
	 * Do for individual sitemap
	 * 
	 * @param sitemap   the sitemap to work on now
	 * 
	 * @param sitemaps  the total list of sitemaps, to add to if needed
	 * 
	 * @param visited   a {@link Set} of visited sitemaps so that we don't crawl
	 *                  again and again
	 * 
	 * @param collector the {@link GenericConsumer} that can be used to collect
	 *                  {@link BurstImage} objects
	 */
	private void doForSitemap(String sitemap, List<String> sitemaps, Set<String> visited,
			GenericConsumer<BurstImage> collector) {
		if (visited.contains(sitemap)) {
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
			if (url.endsWith(".xml")) {
				if (!visited.contains(url)) {
					LOGGER.debug("Adding Shopify Burst sitemap XML: {}", url);
					sitemaps.add(url);
				}
			}

			// check if its a photo
			if (url.startsWith("https://burst.shopify.com/photos/")) {
				// increment stats
				this.imagesFound++;

				// log message
				LOGGER.debug("Found image [{}] url as: {}", this.imagesFound, url);
				
				// induce delay in crawling if desired
				long elapsed = System.currentTimeMillis() - this.lastCrawled;
				long remaining = this.options.delayBetweenImagesMillis - elapsed;
				if(remaining > 0) {
					try {
						LOGGER.debug("Sleeping for {} millis between image fetches", remaining);
						Thread.sleep(remaining);
					} catch(InterruptedException e) {
						// something wants to exit immediately
						return;
					}
				}
				
				this.lastCrawled = System.currentTimeMillis();
				BurstImage crawledImage = this.getBurstImageFromURL(url);
				if (crawledImage != null) {
					boolean continueCrawling = collector.consume(crawledImage);
					if (!continueCrawling) {
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
	 * Read child sitemap files.
	 * 
	 * @return a {@link List} of sitemap files as reported within the
	 *         {@value #MAIN_SITEMAP_FILE}
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

			LOGGER.debug("Discovered child sitemap as: {}", url);
			maps.add(url);
		} while (true);
	}

}
