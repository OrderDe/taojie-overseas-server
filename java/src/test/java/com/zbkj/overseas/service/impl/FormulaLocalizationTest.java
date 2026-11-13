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
  assertEquals("JettaVS5",result.get("modelName").asText());
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
}
