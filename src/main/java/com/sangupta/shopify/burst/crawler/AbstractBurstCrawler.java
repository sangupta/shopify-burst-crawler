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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.consume.GenericConsumer;
import com.sangupta.jerry.http.service.HttpService;
import com.sangupta.jerry.io.AdvancedStringReader;
import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.GsonUtils;

/**
 * Abstract crawler implementation for Shopify Burst. Provides common code
 * for different implementations.
 * 
 * @author sangupta
 *
 */
public abstract class AbstractBurstCrawler {

	/**
	 * My private logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBurstCrawler.class);

	/**
	 * The HTTP service to use
	 */
	@Inject
	protected HttpService httpService;

	/**
	 * Options to use
	 */
	protected final BurstCrawlerOptions options;

	/**
	 * Construct an instance using the provided {@link BurstCrawlerOptions}.
	 * 
	 * @param options {@link BurstCrawlerOptions} to use.
	 */
	public AbstractBurstCrawler(BurstCrawlerOptions options) {
		if (options == null) {
			throw new IllegalArgumentException("BurstCrawlerOptions cannot be null");
		}

		this.options = options;
	}

	/**
	 * The abstract crawling method that all implementations need to provide for a
	 * way to collect streaming results.
	 * 
	 * @param collector the {@link GenericConsumer} based collector
	 */
	public abstract void crawl(GenericConsumer<BurstImage> collector);

	/**
	 * Return a list of all crawled {@link BurstImage}s. This may take a lot of time
	 * as all URLs reachable via the sitemap or the photos page shall be crawled
	 * before the results are returned. For streaming results, use the
	 * {@link #crawl(GenericConsumer)} method.
	 * 
	 * @return {@link List} of {@link BurstImage}s collected
	 */
	public final List<BurstImage> crawl() {
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

	/**
	 * Convert the photo URL such as
	 * 'https://burst.shopify.com/photos/pouring-hot-coffee' to a
	 * {@link CrawledImage} instance.
	 * 
	 * @param url the URL to the page
	 * 
	 * @return the {@link CrawledImage} instance
	 */
	protected BurstImage getBurstImageFromURL(String url) {
		LOGGER.debug("Downloading Shopify photo page: {}", url);

		// download the HTML for image page
		String html = this.httpService.getTextResponse(url);
		if (AssertUtils.isEmpty(html)) {
			LOGGER.debug("Unable to download photo page url: {}", url);
			return null;
		}

		// export image
		final BurstImage image = new BurstImage();
		// copy base values
		image.homeUrl = url;

		if (!this.options.populateDetails) {
			return image;
		}

		// parse and extract data
		this.populateFromHTML(image, html);

		// read name, description from json+ld
		final AdvancedStringReader reader = new AdvancedStringReader(html);
		final String jsonLinkedData = reader.readBetween("<script type=\"application/ld+json\">", "</script>");
		if (AssertUtils.isNotEmpty(jsonLinkedData)) {
			final BurstJsonLinkedData data = GsonUtils.getGson().fromJson(jsonLinkedData, BurstJsonLinkedData.class);

			image.url = data.contentUrl.substring(0, data.contentUrl.indexOf('?'));
			image.title = data.name;
			image.description = data.description;
			image.author = data.author;
			image.licenseUrl = data.license;
		}

		return image;
	}

	/**
	 * Populate the fields of {@link BurstImage} from the HTML page.
	 * 
	 * @param image the {@link BurstImage} to populate
	 * 
	 * @param doc   the JSoup {@link Document} to use
	 */
	protected void populateFromHTML(BurstImage image, String html) {
		final Document doc = Jsoup.parse(html);
		if (doc == null) {
			return;
		}

		Elements elements = doc.select("main");
		if (elements == null) {
			return;
		}

		Element mainNode = elements.first();
		if (mainNode == null) {
			return;
		}

		elements = mainNode.select(".photo__meta a");
		if (elements != null && elements.size() > 0) {
			for (int index = 0; index < elements.size(); index++) {
				Element ele = elements.get(index);
				String href = ele.absUrl("href");
				if (AssertUtils.isEmpty(href)) {
					href = ele.attr("href");
				}

				// populate author url
				if (href.startsWith("https://burst.shopify.com/@")) {
					image.authorUrl = href;
					image.author = ele.text();
					continue;
				}

				// populate license and license url
				if (href.contains("/licenses/")) {
					image.license = ele.text();
					continue;
				}

				// populate tags
				image.tags.add(ele.text());
			}
		}
	}

}
