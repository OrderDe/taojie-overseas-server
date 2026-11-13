package com.zbkj.overseas.service.impl;
import cn.hutool.json.JSONUtil;
import com.zbkj.overseas.common.exception.OverseasException;
import java.util.*;
final class ProductMedia {
 private ProductMedia() {}
 static List<String> read(String json,String legacy) {
  if(json!=null&&!json.isBlank())try{return JSONUtil.toList(json,String.class);}catch(Exception ignored){}
  return legacy==null||legacy.isBlank()?List.of():List.of(legacy);
 }
 static List<String> save(List<String> requested,String existing,String previous,String legacy,int limit) {
  if(requested==null) {
   if(Objects.equals(legacy==null?"":legacy,previous==null?"":previous))return read(existing,previous);
   return validate(legacy==null||legacy.isBlank()?List.of():List.of(legacy),limit);
  }
  return validate(requested,limit);
 }
 static List<String> validate(List<String> urls,int limit) {
  LinkedHashSet<String> unique=new LinkedHashSet<>();
  for(String url:urls){
   if(url==null||url.isBlank())continue;
   String value=url.trim();
   if(value.length()>1024||!(value.matches("(?i)^https?://[^\\s]+$")||value.matches("^/uploads/[^\\s]+$")))throw new OverseasException(400,"媒体地址需为 http/https 或上传文件地址");
   unique.add(value);
  }
  if(unique.size()>limit)throw new OverseasException(400,"媒体数量超过上限："+limit);
  return new ArrayList<>(unique);
 }
}
