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

/**
 * Options that need to be used for {@link BurstCrawler}.
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
    
}
