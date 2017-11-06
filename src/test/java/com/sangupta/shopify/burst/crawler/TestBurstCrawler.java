package com.sangupta.shopify.burst.crawler;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.sangupta.jerry.http.service.impl.DefaultHttpServiceImpl;

public class TestBurstCrawler {
    
    @Test
    public void testCrawler() {
        // declare crawler
        BurstCrawler crawler = new BurstCrawler();
        crawler.setHttpService(new DefaultHttpServiceImpl());
        
        // crawl just one page
        BurstCrawledImages images = crawler.crawl();
        Assert.assertTrue(images.getLastPage() > 100);
        
        // find images
        Assert.assertNotNull(images);
        Assert.assertTrue(images.size() > 0);
        
        // get image details for just one image
        BurstImage image = images.get(0);
        crawler.populateImageData(Arrays.asList(new BurstImage[] { image }));
        
        Assert.assertNotNull(image.author);
        Assert.assertNotNull(image.authorUrl);
        Assert.assertNotNull(image.description);
        Assert.assertNotNull(image.homeUrl);
        Assert.assertNotNull(image.license);
        Assert.assertNotNull(image.title);
        Assert.assertNotNull(image.url);
        Assert.assertNotNull(image.tags);
        Assert.assertTrue(image.tags.size() > 0);        
    }

}
