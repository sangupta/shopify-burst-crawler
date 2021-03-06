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

import java.util.HashSet;
import java.util.Set;

/**
 * Options that can to be used for {@link BurstCrawler}.
 * 
 * @author sangupta
 *
 */
public class BurstCrawlerOptions {
    
    /**
     * The maximum number of pages to crawl
     */
    public int maxPages = -1;
    
    /**
     * The start page
     */
    public int startPage = 1;
    
    /**
     * The end page
     */
    public int endPage = -1;

    /**
     * Maximum number of images to crawl
     */
    public int maxImages = -1;
    
    /**
     * Time delay between fetching different pages
     */
    public int delayBetweenPagesMillis = 1000;
    
    /**
     * Time delay between fetching image pages
     */
    public int delayBetweenImagesMillis = 1000;
        
    /**
     * Indicates if we need to populate each detail of each image
     */
    public boolean populateDetails = true;
    
    /**
     * URL {@link Set} of previously crawled images. It can be used
     * to prevent crawling of these again. Any URL added here will
     * not be reported in the resulting set.
     */
    public final Set<String> previouslyCrawled = new HashSet<>();
    
    public BurstCrawlerOptions setMaxPages(int pages) {
        this.maxPages = pages;
        return this;
    }

    public BurstCrawlerOptions setStartPage(int page) {
        this.startPage = page;
        return this;
    }
    
    public BurstCrawlerOptions setEndPage(int page) {
        this.endPage = page;
        return this;
    }
    
    public BurstCrawlerOptions setMaxImages(int images) {
        this.maxImages = images;
        return this;
    }
    
    public BurstCrawlerOptions setDelayBetweenPagesMillis(int delay) {
        this.delayBetweenPagesMillis = delay;
        return this;
    }
    
    public BurstCrawlerOptions setDelayBetweenImagesMillis(int delay) {
        this.delayBetweenImagesMillis = delay;
        return this;
    }
    
}
