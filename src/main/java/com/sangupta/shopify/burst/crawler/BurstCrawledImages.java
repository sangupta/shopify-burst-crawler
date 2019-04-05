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
import java.util.List;

public class BurstCrawledImages {

    public final List<BurstImage> images = new ArrayList<>();
    
    int lastPage;
    
    public void add(BurstImage image) {
        if(image == null) {
            return;
        }
        
        this.images.add(image);
    }
    
    public int size() {
        return this.images.size();
    }
    
    public BurstImage get(int index) {
        return this.images.get(index);
    }

    public int getLastPage() {
        return this.lastPage;
    }
    
}
