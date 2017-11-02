# shopify-burst-crawler

Simple crawler to download meta information for all stock pics from Shopify Burst 
website: https://burst.shopify.com.

## Usage

The `junit` test case below depicts on how the crawler can be used within another
application:

```java
@Test
public void testCrawler() {
    // declare crawler
    BurstCrawler crawler = new BurstCrawler();
    
    // wire the HttpService to use
    crawler.setHttpService(new DefaultHttpServiceImpl());
    
    // crawl just one page
    // pass a value of zero or negative to crawl all pages available
    crawler.crawl(1);

    // or you may use the following method to crawl all images
    // by scanning and moving across pages
    crawler.crawl();

    Assert.assertTrue(crawler.getLastPage() > 100);
    
    // find images that were crawled
    List<BurstImage> images = crawler.getImages();
    Assert.assertNotNull(images);
    Assert.assertTrue(images.size() > 0);
    
    // get image details for just one image
    BurstImage image = images.get(0);

    // this takes in a list of all images that need to be
    // prepopulated metadata for given images
    // this method is not dependent on the crawler.crawl() method
    // and thus can be used independently
    crawler.populateImageData(Arrays.asList(new BurstImage[] { image }));

    // you may also use the following method to populate
    // metadata for all images that were just crawled by this
    // crawler instance
    crawler.populateImageData();
    
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
```

## Downloads

The current development snapshot `JAR` can be obtained using `JitPack.io` as:

Add the following `repository` to Maven,

```xml
<repository>
	<id>jitpack.io</id>
	<url>https://jitpack.io</url>
</repository>
```

Then add the dependency as,

```xml
<dependency>
    <groupId>com.github.sangupta</groupId>
    <artifactId>shopify-burst-crawler</artifactId>
    <version>-SNAPSHOT</version>
</dependency>
```

## Versioning

For transparency and insight into our release cycle, and for striving to maintain backward compatibility, 
this project will be maintained under the Semantic Versioning guidelines as much as possible.

Releases will be numbered with the follow format:

`<major>.<minor>.<patch>`

And constructed with the following guidelines:

* Breaking backward compatibility bumps the major
* New additions without breaking backward compatibility bumps the minor
* Bug fixes and misc changes bump the patch

For more information on SemVer, please visit http://semver.org/.

## License
	
```
shopify-burst-crawler
Copyright (c) 2017, Sandeep Gupta

https://sangupta.com/projects/shopify-burst-crawler

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
