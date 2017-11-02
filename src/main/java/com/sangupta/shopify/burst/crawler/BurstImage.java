package com.sangupta.shopify.burst.crawler;

import java.util.ArrayList;
import java.util.List;

/**
 * Value object representing data about single burst image.
 * 
 * @author sangupta
 *
 */
public class BurstImage {

    public String url;

    public String homeUrl;

    public String title;

    public String description;

    public String author;

    public String authorUrl;

    public String license;

    public final List<String> tags = new ArrayList<>();

    @Override
    public int hashCode() {
        if(this.url == null) {
            return 0;
        }
        
        return this.url.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        
        if(this == obj) {
            return true;
        }
        
        if(this.url == null) {
            return false;
        }
        
        if(obj instanceof BurstImage) {
            BurstImage other = (BurstImage) obj;
            return this.url.equals(other.url);
        }
        
        if(obj instanceof String) {
            return this.url.equals((String) obj);
        }

        return false;
    }

    @Override
    public String toString() {
        return "[BurstImage: " + this.url + "]";
    }
    
}
