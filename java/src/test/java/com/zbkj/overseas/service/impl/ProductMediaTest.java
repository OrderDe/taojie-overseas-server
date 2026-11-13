package com.zbkj.overseas.service.impl;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProductMediaTest {
 @Test void legacySingleMediaRemainsVisible(){assertEquals(List.of("https://a/a.jpg"),ProductMedia.read(null,"https://a/a.jpg"));}
 @Test void explicitEmptyListClearsMedia(){assertTrue(ProductMedia.read("[]","https://a/a.jpg").isEmpty());assertTrue(ProductMedia.save(List.of(),"[\"https://a/a.jpg\"]","https://a/a.jpg","https://a/a.jpg",20).isEmpty());}
 @Test void oldClientPreservesGalleryUntilCoverChanges(){assertEquals(List.of("https://a/a.jpg","https://a/b.jpg"),ProductMedia.save(null,"[\"https://a/a.jpg\",\"https://a/b.jpg\"]","https://a/a.jpg","https://a/a.jpg",20));assertEquals(List.of("https://a/c.jpg"),ProductMedia.save(null,"[]","https://a/a.jpg","https://a/c.jpg",20));}
 @Test void deduplicatesWithoutChangingOrder(){assertEquals(List.of("https://a/b.jpg","https://a/a.jpg"),ProductMedia.validate(List.of("https://a/b.jpg","https://a/a.jpg","https://a/b.jpg"),20));}
 @Test void rejectsInvalidUrlsAndExcessItems(){assertThrows(RuntimeException.class,()->ProductMedia.validate(List.of("javascript:alert(1)"),20));assertThrows(RuntimeException.class,()->ProductMedia.validate(List.of("https://a/a","https://a/b"),1));}
}
