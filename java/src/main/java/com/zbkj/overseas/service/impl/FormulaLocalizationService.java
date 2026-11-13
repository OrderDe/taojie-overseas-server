package com.zbkj.overseas.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zbkj.overseas.common.exception.OverseasException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/** Display text only: codes, IDs and measurements stay exact. */
@Service
public class FormulaLocalizationService {
    private static final Set<String> LANGS = Set.of("zh","en","es","fr","pt","id","ms","vi","th","ar","ru","de");
    private static final Set<String> FIELDS = Set.of("name","model","brandName","modelName","carColor","colorName");
    private static final Pattern HAN = Pattern.compile("[\\p{IsHan}]+");
    private static final Map<String,String> BRANDS = Map.ofEntries(
        Map.entry("一汽大众","FAW-Volkswagen"), Map.entry("一汽","FAW"), Map.entry("大众","Volkswagen"),
        Map.entry("捷达","Jetta"), Map.entry("奥迪","Audi"), Map.entry("宝马","BMW"),
        Map.entry("奔驰","Mercedes-Benz"), Map.entry("比亚迪","BYD"), Map.entry("丰田","Toyota"),
        Map.entry("本田","Honda"), Map.entry("日产","Nissan"), Map.entry("福特","Ford"),
        Map.entry("马自达","Mazda"), Map.entry("沃尔沃","Volvo"), Map.entry("雪佛兰","Chevrolet"),
        Map.entry("保时捷","Porsche"), Map.entry("特斯拉","Tesla"), Map.entry("雷克萨斯","Lexus"));
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${LIBRETRANSLATE_URL:http://127.0.0.1:5000}") private String libreUrl;
    public FormulaLocalizationService(JdbcTemplate jdbc,ObjectMapper json) { this.jdbc=jdbc; this.json=json; }

    static String language(String lang) {
        String result=lang==null?"zh":lang.toLowerCase(Locale.ROOT).split("[-_]")[0];
        return LANGS.contains(result)?result:"zh";
    }
    static JsonNode rewrite(JsonNode node,UnaryOperator<String> translate) {
        return rewriteFields(node,(field,text)->translate.apply(text));
    }
    private static JsonNode rewriteFields(JsonNode node,java.util.function.BiFunction<String,String,String> translate) {
        if(node.isArray()) node.forEach(child->rewriteFields(child,translate));
        else if(node.isObject()) {
            ObjectNode object=(ObjectNode)node;
            List<String> keys=new ArrayList<>(); object.fieldNames().forEachRemaining(keys::add);
            for(String key:keys) {
                JsonNode value=object.get(key);
                if(value.isTextual() && FIELDS.contains(key)) object.put(key,translate.apply(key,value.asText()));
                else if(value.isContainerNode()) rewriteFields(value,translate);
            }
        }
        return node;
    }
    static String segments(String text,UnaryOperator<String> translate) {
        return HAN.matcher(text).replaceAll(match->java.util.regex.Matcher.quoteReplacement(
            BRANDS.containsKey(match.group())?BRANDS.get(match.group()):translate.apply(match.group())));
    }
    private static String displayText(String field,String text,UnaryOperator<String> translate) {
        if ((field.equals("carColor") || field.equals("colorName")) && !HAN.matcher(text).find() && text.matches(".*[A-Za-z].*")) return translate.apply("en:"+text);
        return segments(text,source->translate.apply(source.equals("香薰灰")?"en:Aromatherapy Grey":source));
    }
    public synchronized JsonNode localize(Object data,String requestedLang) {
        JsonNode tree=json.valueToTree(data);
        String lang=language(requestedLang);
        if(lang.equals("zh")) return tree;
        Set<String> sources=new LinkedHashSet<>();
        rewriteFields(tree.deepCopy(),(field,text)->displayText(field,text,source->{sources.add(source);return source;}));
        Map<String,String> translations=new HashMap<>();
        List<String> missing=new ArrayList<>();
        for(String source:sources) {
            List<String> saved=jdbc.query("SELECT translated_text FROM eb_overseas_formula_translation WHERE source_hash=? AND lang=? AND source_text=?",
                (rs,n)->rs.getString(1),hash(source),lang,source);
            if(saved.isEmpty()) missing.add(source); else translations.put(source,saved.get(0));
        }
        missing.sort(java.util.Comparator.comparing(source->source.startsWith("en:")));
        for(int offset=0;offset<missing.size();) {
            int end=offset+1;
            while(end<missing.size() && end-offset<32 && missing.get(end).startsWith("en:")==missing.get(offset).startsWith("en:")) end++;
            List<String> batch=missing.subList(offset,end);
            offset=end;
            List<String> outputs=translateBatch(batch,lang);
            for(int i=0;i<batch.size();i++) {
                String source=batch.get(i),translated=outputs.get(i);
                jdbc.update("INSERT INTO eb_overseas_formula_translation(source_hash,lang,source_text,translated_text) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE translated_text=VALUES(translated_text)",hash(source),lang,source,translated);
                translations.put(source,translated);
            }
        }
        return rewriteFields(tree,(field,text)->displayText(field,text,source->translations.getOrDefault(source,source)));
    }
    private List<String> translateBatch(List<String> batch,String lang) {
        try {
            String source=batch.get(0).startsWith("en:")?"en":"zh";
            List<String> input=batch.stream().map(value->source.equals("en")?value.substring(3):value).toList();
            if(source.equals(lang)) return input;
            Map<String,Object> body=new HashMap<>(Map.of("q",input,"source",source,"target",lang,"format","text"));
            String apiKey=System.getenv("LIBRETRANSLATE_API_KEY");
            if(apiKey!=null&&!apiKey.isBlank()) body.put("api_key",apiKey);
            HttpRequest req=HttpRequest.newBuilder(URI.create(libreUrl.replaceAll("/+$","")+"/translate"))
                .timeout(Duration.ofSeconds(45)).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response=http.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new IllegalStateException("Translation HTTP "+response.statusCode());
            JsonNode values=json.readTree(response.body()).get("translatedText");
            if(values==null||!values.isArray()||values.size()!=batch.size()) throw new IllegalStateException("Invalid translation batch");
            List<String> result=new ArrayList<>();
            for(JsonNode value:values) {
                String out=value.asText().trim();
                if(out.isEmpty()||HAN.matcher(out).find()) throw new IllegalStateException("Incomplete translation");
                result.add(out);
            }
            return result;
        } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new OverseasException(503,"Formula translation interrupted"); }
        catch(Exception e) { throw new OverseasException(503,"Formula translation unavailable: "+e.getMessage()); }
    }
    private static String hash(String text) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
}
