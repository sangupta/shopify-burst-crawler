# shopify-burst-crawler

Simple crawlers to download meta information for all stock photos from Shopify Burst 
website: https://burst.shopify.com. There are 2 crawling modes supported:

* BurstCrawler - starts crawling using the latest photos URL
* BurstSitemapCrawler - starts crawling using the sitemap URL

## Usage

Both crawlers support same API:

```java
BurstCrawler crawler = new BurstCrawler();

// or you could use the Sitemap crawler
crawler = new BurstSitemapCrawler();

// this will crawl and return the entire bunch of results
// it is slow and may take a lot of time
List<BurstImage> images = crawler.crawl();

// a streaming version if also available
GenericConsumer<BurstImage> collector = new GenericConsumer<BurstImage>() {

	@Override
	public boolean consume(BurstImage image) {
		// do something with the image
		// ...
		
		// return a true if you want to continue crawling
		// or return, a false to stop the crawling
		return true;
	}
};
crawler.crawl(collector);
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
    <version>1.0.0-SNAPSHOT</version>
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
Copyright (c) 2017-2019, Sandeep Gupta

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
