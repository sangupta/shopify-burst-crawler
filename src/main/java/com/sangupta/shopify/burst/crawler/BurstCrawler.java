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

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.http.WebResponse;
import com.sangupta.jerry.http.service.HttpService;
import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.StringUtils;

/**
 * Simple CLI tool to crawl Shopify Burst image site and provide
 * a list of all images and their details.
 * 
 * @author sangupta
 *
 */
public class BurstCrawler {

    /**
     * My private logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BurstCrawler.class);

    /**
     * Base URL to site
     */
    private static final String BASE_URL = "https://burst.shopify.com/photos";

    /**
     * Options to use
     */
    private final BurstCrawlerOptions options;

    /**
     * The HTTP service to use
     */
    @Inject
    private HttpService httpService;
    
    /**
     * Create {@link BurstCrawler} instance with default {@link BurstCrawlerOptions}.
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
        if(options == null) {
            throw new IllegalArgumentException("BurstCrawlerOptions cannot be null");
        }
        
        this.options = options;
    }
    
    /**
     * Crawl the entire site for all pages on the site.
     * 
     * @return a list of {@link BurstImage} discovered from crawling
     */
    public BurstCrawledImages crawl() {        
        BurstCrawledImages images = new BurstCrawledImages();
        
        int currentPage = options.startPage;
        int crawled = 1;
        do {
            doForPage(images, options, currentPage);
            
            if(images.size() == options.maxImages) {
                LOGGER.debug("Max images reached, breaking from crawling more images");
                break;
            }
            
            if(crawled == options.maxPages) {
                LOGGER.debug("Max pages reached, breaking from crawling more images");
                break;
            }
            
            if(currentPage == options.endPage) {
                LOGGER.debug("Last page limit reached, breaking from crawling more images");
                break;
            }
            
            if(options.delayBetweenPagesMillis > 0) {
                sleepQuietly(options.delayBetweenPagesMillis);
            }

            currentPage++;
            crawled++;
        } while (currentPage < images.lastPage);
        
        LOGGER.debug("Total number of images crawled: {}", images.size());
        
        return images;
    }

    /**
     * Make this thread sleep for a while.
     * 
     * @param delay
     */
    private void sleepQuietly(int delay) {
        if(delay <= 0) {
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
        if(mainNode == null) {
            return;
        }
        
        // populate
        image.title = mainNode.select("h1.heading--2").text();
        image.description = mainNode.select("p.photo-info__description").text();
        
        // meta tags
        Elements elements = mainNode.select(".photo__meta a");
        if(elements != null && elements.size() > 0) {
            for(int index = 0; index < elements.size(); index++) {
                Element ele = elements.get(index);
                String href = ele.absUrl("href");
                
                if(href.contains("/@")) {
                    image.authorUrl = href;
                    image.author = ele.text();
                    continue;
                }
                
                if(href.contains("/tags/")) {
                    image.tags.add(ele.text());
                    continue;
                }
                
                if(href.contains("/licenses/")) {
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
     * @param images 
     * @param options 
     * 
     * @param page
     */
    private void doForPage(BurstCrawledImages images, BurstCrawlerOptions options, int page) {
        LOGGER.debug("Crawling page: {}", page);
        String url = BASE_URL;
        if (page > 1) {
            url = url + "?page=" + page;
        }

        Document doc = this.getHtmlDoc(url);
        if (doc == null) {
            return;
        }

        if (page == 1) {
            extractLastPage(images, doc);
        }

        getPhotosFromPage(images, options, doc);
    }

    /**
     * Get basic info on photos from the given page document
     * @param images 
     * @param options 
     * 
     * @param doc
     */
    private void getPhotosFromPage(BurstCrawledImages images, BurstCrawlerOptions options, Document doc) {
        // clear up noise
        Element mainNode = getMainNode(doc);
        if(mainNode == null) {
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

            BurstImage image = new BurstImage();
            image.homeUrl = url;

            images.add(image);
            
            if(images.size() == options.maxImages) {
                break;
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
    private void extractLastPage(BurstCrawledImages images, Document doc) {
        LOGGER.debug("Extracting last page from HTML");

        // get last page URL so that we can run a loop
        Elements elements = doc.select("span.last a");
        if (elements != null) {
            String href = elements.first().absUrl("href");
            int index = href.indexOf('=');
            if (index > 0) {
                String num = href.substring(index + 1);
                int pageNum = StringUtils.getIntValue(num, -1);
                if (pageNum > 0) {
                    LOGGER.info("Last page detected as: {}", pageNum);
                    images.lastPage = pageNum;
                }
            }
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
