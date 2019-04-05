/**
 *
 * shopify-burst-crawler: Java Client for burst.shopify.com API
 * Copyright (c) 2017, Sandeep Gupta
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

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.consume.GenericConsumer;
import com.sangupta.jerry.http.WebResponse;
import com.sangupta.jerry.http.service.HttpService;
import com.sangupta.jerry.io.AdvancedStringReader;
import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.StringUtils;

/**
 * Simple CLI tool to crawl Shopify Burst image site and provide a list of all
 * images and their details.
 * 
 * @author sangupta
 *
 */
public class BurstCrawler extends AbstractBurstCrawler {

	/**
	 * My private logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BurstCrawler.class);

	/**
	 * Base URL to site
	 */
	private static final String BASE_URL = "https://burst.shopify.com/photos?sort=latest";

	/**
	 * Last page as detected
	 */
	private int lastPage = 0;

	/**
	 * Total images collected till now
	 */
	private int totalCollected = 0;

	/**
	 * Create {@link BurstCrawler} instance with default
	 * {@link BurstCrawlerOptions}.
	 * 
	 */
	public BurstCrawler() {
		this(new BurstCrawlerOptions());
	}

	/**
	 * Create {@link BurstCrawler} instance with given {@link BurstCrawlerOptions}.
	 * 
	 * @param options
	 */
	public BurstCrawler(BurstCrawlerOptions options) {
		super(options);

		this.lastPage = 0;
		this.totalCollected = 0;
	}

	/**
	 * Start crawling using the given {@link GenericConsumer} collector.
	 * 
	 * @param collector the {@link GenericConsumer} that will consume the crawled
	 *                  {@link BurstImage}s
	 */
	public void crawl(GenericConsumer<BurstImage> collector) {
		int currentPage = this.options.startPage;
		int crawled = 1;
		do {
			doForPage(collector, currentPage);

			if (this.totalCollected == options.maxImages) {
				LOGGER.debug("Max images reached, breaking from crawling more images");
				break;
			}

			if (crawled == options.maxPages) {
				LOGGER.debug("Max pages reached, breaking from crawling more images");
				break;
			}

			if (currentPage == options.endPage) {
				LOGGER.debug("Last page limit reached, breaking from crawling more images");
				break;
			}

			if (options.delayBetweenPagesMillis > 0) {
				sleepQuietly(options.delayBetweenPagesMillis);
			}

			currentPage++;
			crawled++;
		} while (currentPage < this.lastPage);

		LOGGER.debug("Total number of images crawled: {}", this.totalCollected);
	}

	/**
	 * Make this thread sleep for a while.
	 * 
	 * @param delay
	 */
	private void sleepQuietly(int delay) {
		if (delay <= 0) {
			return;
		}

		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// eat up
		}
	}

	/**
	 * Populate image details for given list of Burst images.
	 * 
	 * @param images
	 */
	public void populateImageData(List<BurstImage> images) {
		if (AssertUtils.isEmpty(images)) {
			return;
		}

		LOGGER.info("Request to populate image data for {} images", images.size());

		for (int index = 0; index < images.size(); index++) {
			BurstImage image = images.get(index);
			LOGGER.debug("[{}] Populating image data for url: {}", index, image.homeUrl);

			Document doc = this.getHtmlDoc(image.homeUrl);
			if (doc == null) {
				continue;
			}

			this.populateImageDetails(image, doc);

			this.sleepQuietly(this.options.delayBetweenImagesMillis);
		}
	}

	/**
	 * Populate image details from HTML selector onto object.
	 * 
	 * @param image
	 * @param doc
	 */
	private void populateImageDetails(BurstImage image, Document doc) {
		Element mainNode = this.getMainNode(doc);
		if (mainNode == null) {
			return;
		}

		// populate
		image.title = mainNode.select("h1.heading--2").text();
		image.description = mainNode.select("p.photo-info__description").text();

		// meta tags
		Elements elements = mainNode.select(".photo__meta a");
		if (elements != null && elements.size() > 0) {
			for (int index = 0; index < elements.size(); index++) {
				Element ele = elements.get(index);
				String href = ele.absUrl("href");

				if (href.contains("/@")) {
					image.authorUrl = href;
					image.author = ele.text();
					continue;
				}

				if (href.contains("/tags/")) {
					image.tags.add(ele.text());
					continue;
				}

				if (href.contains("/licenses/")) {
					image.license = ele.text();
					continue;
				}
			}
		}

		// download URL
		image.url = image.homeUrl + "/download";
	}

	/**
	 * Run crawler & parser over the given page index.
	 * 
	 * @param images
	 * @param options
	 * 
	 * @param page
	 */
	private void doForPage(GenericConsumer<BurstImage> collector, int page) {
		LOGGER.debug("Crawling page: {}", page);
		String url = BASE_URL;
		if (page > 1) {
			url = url + "&page=" + page;
		}

		Document doc = this.getHtmlDoc(url);
		if (doc == null) {
			return;
		}

		if (page == 1) {
			extractLastPage(doc);
		}

		getPhotosFromPage(collector, options, doc);
	}

	/**
	 * Get basic info on photos from the given page document
	 * 
	 * @param collector
	 * @param options
	 * 
	 * @param doc
	 */
	private void getPhotosFromPage(GenericConsumer<BurstImage> collector, BurstCrawlerOptions options, Document doc) {
		// clear up noise
		Element mainNode = getMainNode(doc);
		if (mainNode == null) {
			return;
		}

		// start selecting pics
		Elements links = mainNode.select("a.photo-tile__image-wrapper");
		if (links == null) {
			LOGGER.debug("No images on page");
			return;
		}

		// loop over
		LOGGER.debug("Found num images in page: {}", links.size());
		for (int index = 0; index < links.size(); index++) {
			String url = links.get(index).absUrl("href");
			if (AssertUtils.isEmpty(url)) {
				continue;
			}

			final BurstImage image = this.getBurstImageFromURL(url);
			this.totalCollected++;

			boolean continueCrawling = collector.consume(image);
			if (!continueCrawling) {
				return;
			}

			if (this.totalCollected == options.maxImages) {
				return;
			}
		}
	}

	/**
	 * Select the <code>main</code> tag from the HTML document.
	 * 
	 * @param doc
	 * @return
	 */
	private Element getMainNode(Document doc) {
		Elements elements = doc.select("main");
		if (elements == null) {
			return null;
		}

		// get main node
		return elements.first();
	}

	/**
	 * Extract last page details from the page.
	 * 
	 * @param doc
	 */
	private void extractLastPage(Document doc) {
		LOGGER.debug("Extracting last page from HTML");

		// get last page URL so that we can run a loop
		Elements elements = doc.select("span.last a");
		if (elements == null || elements.isEmpty()) {
			return;
		}

		String href = elements.first().absUrl("href");
		AdvancedStringReader reader = new AdvancedStringReader(href);

		String num = reader.readBetween("page=", "&");
		int pageNum = StringUtils.getIntValue(num, -1);
		if (pageNum > 0) {
			LOGGER.info("Last page detected as: {}", pageNum);
			this.lastPage = pageNum;
		}
	}

	/**
	 * Parse HTML from URL to a JSOUP document.
	 * 
	 * @param url
	 * @return
	 */
	private Document getHtmlDoc(String url) {
		String html = this.getHtml(url);
		if (AssertUtils.isEmpty(html)) {
			return null;
		}

		return Jsoup.parse(html, url);
	}

	/**
	 * Get HTML content for the URL.
	 * 
	 * @param url
	 * @return
	 */
	private String getHtml(String url) {
		LOGGER.debug("Fetching HTML response from URL: {}", url);
		WebResponse response = this.httpService.getResponse(url);
		if (response == null || !response.isSuccess()) {
			return null;
		}

		return response.getContent();
	}

	// Usual accessors follow

	/**
	 * Allow to set the {@link HttpService} to be used.
	 * 
	 * @param httpService
	 */
	public void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}

}
