package com.zbkj.overseas.service.impl;
import com.aliyun.oss.*;
import com.aliyun.oss.model.*;
import com.zbkj.overseas.common.exception.OverseasException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;
@Service
public class OssMediaStorage {
 @Value("\u0024{overseas.oss.enabled:false}") private boolean enabled;
 @Value("\u0024{overseas.oss.endpoint:}") private String endpoint;
 @Value("\u0024{overseas.oss.bucket:}") private String bucket;
 @Value("\u0024{overseas.oss.access-key-id:}") private String accessId;
 @Value("\u0024{overseas.oss.access-key-secret:}") private String accessSecret;
 @Value("\u0024{overseas.oss.public-url:}") private String publicUrl;
 @Value("\u0024{overseas.oss.prefix:taojie-overseas/media/}") private String prefix;
 public boolean isEnabled(){return enabled;}
 public String upload(byte[] bytes,String ext) {
  if(endpoint.isBlank()||bucket.isBlank()||accessId.isBlank()||accessSecret.isBlank())throw new OverseasException(503,"OSS 上传配置不完整");
  String host=endpoint.replaceFirst("^https?://","").replaceAll("/+$","");
  String key=prefix.replaceAll("^/+|/+$","")+"/"+LocalDate.now()+"/"+UUID.randomUUID()+ext;
  OSS client=new OSSClientBuilder().build("https://"+host,accessId,accessSecret);
  try {
   ObjectMetadata metadata=new ObjectMetadata();metadata.setContentLength(bytes.length);
   metadata.setContentType(switch(ext){case ".png"->"image/png";case ".webp"->"image/webp";case ".gif"->"image/gif";case ".mp4"->"video/mp4";case ".webm"->"video/webm";case ".mov"->"video/quicktime";default->"image/jpeg";});
   client.putObject(new PutObjectRequest(bucket,key,new ByteArrayInputStream(bytes),metadata));
   String base=publicUrl.isBlank()?"https://"+bucket+"."+host:publicUrl;
   if(!base.matches("^https?://.*"))base="https://"+base;
   return base.replaceAll("/+$","")+"/"+key;
  }catch(Exception e){throw new OverseasException(502,"OSS 上传失败，请检查存储配置和网络");}finally{client.shutdown();}
 }
}
