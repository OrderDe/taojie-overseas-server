package com.zbkj.overseas.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FormulaLocalizationTest {
 @Test void translatesDisplayFieldsWithoutChangingCodesIdsOrQuantities() throws Exception {
  var json=new ObjectMapper();
  var data=json.readTree("{\"id\":\"1991416668565368832\",\"carColor\":\"香薰灰\",\"colorNum\":\"LU7B\",\"brandName\":\"一汽大众\",\"modelName\":\"捷达VS5\",\"numbers\":[{\"name\":\"LU7B 香薰灰2019\",\"formula\":[{\"model\":\"1K187\",\"name\":\"珍珠\",\"dose\":12.34}]}]}");
  var result=FormulaLocalizationService.rewrite(data,text->FormulaLocalizationService.segments(text,s->s.equals("香薰灰")?"Gris aromático":"Perla"));
  assertEquals("Gris aromático",result.get("carColor").asText());
  assertEquals("FAW-Volkswagen",result.get("brandName").asText());
  assertEquals("Jetta VS5",result.get("modelName").asText());
  assertEquals("LU7B",result.get("colorNum").asText());
  assertEquals("1991416668565368832",result.get("id").asText());
  assertEquals("LU7B Gris aromático2019",result.at("/numbers/0/name").asText());
  assertEquals("1K187",result.at("/numbers/0/formula/0/model").asText());
  assertEquals(12.34,result.at("/numbers/0/formula/0/dose").asDouble());
 }
 @Test void chineseLeavesOriginalDataUnchanged() {
  var service=new FormulaLocalizationService(null,new ObjectMapper());
  assertEquals("香薰灰",service.localize(java.util.Map.of("carColor","香薰灰"),"zh-CN").get("carColor").asText());
 }
 @Test void normalizesOnlySupportedLocales() {
  assertEquals("es",FormulaLocalizationService.language("es-ES"));
  assertEquals("zh",FormulaLocalizationService.language("bogus"));
 }
 @Test void segmentsSplitsBrandPrefixAndTranslatesOnlyTheRest() {
  var seen=new java.util.ArrayList<String>();
  var result=FormulaLocalizationService.segments("五菱宏光",s->{seen.add(s);return s.equals("宏光")?"Hongguang":s;});
  assertEquals("Wuling Hongguang",result);
  assertEquals(java.util.List.of("宏光"),seen);
 }
 @Test void segmentsUsesLongestBrandMatch() {
  assertEquals("FAW-Volkswagen",FormulaLocalizationService.segments("一汽大众",s->s));
  assertEquals("Changan Suzuki",FormulaLocalizationService.segments("长安铃木",s->s));
  assertEquals("SAIC Volkswagen",FormulaLocalizationService.segments("上汽大众",s->s));
  assertEquals("Dongfeng Honda",FormulaLocalizationService.segments("东风本田",s->s));
 }
 @Test void segmentsSpacesBrandAgainstLatinNeighbours() {
  assertEquals("Geely ICON",FormulaLocalizationService.segments("吉利ICON",s->s));
  assertEquals("Wuling Geely",FormulaLocalizationService.segments("五菱吉利",s->s));
  var result=FormulaLocalizationService.segments("宏光五菱",s->s.equals("宏光")?"Hongguang":s);
  assertEquals("Hongguang Wuling",result);
 }
 @Test void segmentsLeavesNonBrandRunsAndNonHanTextAlone() {
  var seen=new java.util.ArrayList<String>();
  assertEquals("珍珠白",FormulaLocalizationService.segments("珍珠白",s->{seen.add(s);return s;}));
  assertEquals("1K187",FormulaLocalizationService.segments("1K187",s->{seen.add(s);return s;}));
  assertEquals(java.util.List.of("珍珠白"),seen);
 }
 @Test void segmentsCollectsSplitSegmentsAcrossMixedText() {
  var seen=new java.util.ArrayList<String>();
  var result=FormulaLocalizationService.segments("五菱宏光PLUS五菱",s->{seen.add(s);return s;});
  assertEquals(java.util.List.of("宏光"),seen);
  assertEquals("Wuling宏光PLUS Wuling",result);
 }
}
