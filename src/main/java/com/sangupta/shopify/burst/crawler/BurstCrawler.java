package com.sangupta.shopify.burst.crawler;

import java.util.ArrayList;
import java.util.List;

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
     * The HTTP service to use
     */
    private HttpService httpService;

    /**
     * Holds index to last page
     */
    private int lastPage;

    /**
     * Holds a list of all crawled image details
     */
    private final List<BurstImage> images = new ArrayList<>();

    /**
     * Crawl the entire site.
     * 
     */
    public void crawl() {
        this.crawl(-1);
    }
    
    public void crawl(int maxPages) {
        int currentPage = 1;
        do {
            doForPage(currentPage);
            
            if(currentPage == maxPages) {
                LOGGER.debug("Max pages reached, breaking from crawling more images");
                break;
            }

            currentPage++;
        } while (currentPage < this.lastPage);
        
        LOGGER.debug("Total number of images crawled: {}", this.images.size());
    }

    /**
     * Populate details of all images that were just crawled by
     * this instance of the client.
     * 
     */
    public void populateImageData() {
        LOGGER.debug("Request to populate image data for all images in this crawler instance");
        this.populateImageData(this.images);
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

        for (BurstImage image : images) {
            LOGGER.debug("Populating image data for url: {}", image.homeUrl);
            
            Document doc = this.getHtmlDoc(image.homeUrl);
            if (doc == null) {
                continue;
            }

            this.populateImageDetails(image, doc);
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
     * 
     * @param page
     */
    private void doForPage(int page) {
        LOGGER.debug("Crawling page: {}", page);
        String url = BASE_URL;
        if (page > 1) {
            url = url + "?page=" + page;
        }

        Document doc = this.getHtmlDoc(BASE_URL);
        if (doc == null) {
            return;
        }

        if (page == 1) {
            extractLastPage(doc);
        }

        getPhotosFromPage(doc);
    }

    /**
     * Get basic info on photos from the given page document
     * 
     * @param doc
     */
    private void getPhotosFromPage(Document doc) {
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

            this.images.add(image);
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
        if (elements != null) {
            String href = elements.first().absUrl("href");
            int index = href.indexOf('=');
            if (index > 0) {
                String num = href.substring(index + 1);
                int pageNum = StringUtils.getIntValue(num, -1);
                if (pageNum > 0) {
                    LOGGER.info("Last page detected as: {}", pageNum);
                    this.lastPage = pageNum;
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
        WebResponse response = this.httpService.getResponse(url);
        if (response == null || !response.isSuccess()) {
            return null;
        }

        return response.getContent();
    }
    
    // Usual accessors follow

    public List<BurstImage> getImages() {
        return this.images;
    }
    
    public int getLastPage() {
        return this.lastPage;
    }
    
    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }
    
}
