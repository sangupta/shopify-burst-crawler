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
        if(this.homeUrl == null) {
            return 0;
        }
        
        return this.homeUrl.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        
        if(this == obj) {
            return true;
        }
        
        if(this.homeUrl == null) {
            return false;
        }
        
        if(obj instanceof BurstImage) {
            BurstImage other = (BurstImage) obj;
            return this.homeUrl.equals(other.homeUrl);
        }
        
        if(obj instanceof String) {
            return this.homeUrl.equals((String) obj);
        }

        return false;
    }

    @Override
    public String toString() {
        return "[BurstImage: " + this.homeUrl + "]";
    }
    
}
