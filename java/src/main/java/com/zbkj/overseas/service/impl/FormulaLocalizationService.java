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
import java.util.regex.Matcher;
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
        Map.entry("保时捷","Porsche"), Map.entry("特斯拉","Tesla"), Map.entry("雷克萨斯","Lexus"),
        Map.entry("五菱","Wuling"), Map.entry("东风","Dongfeng"), Map.entry("吉利","Geely"),
        Map.entry("长安","Changan"), Map.entry("长城","Great Wall"), Map.entry("哈弗","Haval"),
        Map.entry("奇瑞","Chery"), Map.entry("红旗","Hongqi"), Map.entry("荣威","Roewe"),
        Map.entry("名爵","MG"), Map.entry("宝骏","Baojun"), Map.entry("传祺","Trumpchi"),
        Map.entry("广汽","GAC"), Map.entry("北汽","BAIC"), Map.entry("江淮","JAC"),
        Map.entry("上汽","SAIC"), Map.entry("上汽大通","Maxus"), Map.entry("大通","Maxus"),
        Map.entry("蔚来","NIO"), Map.entry("小鹏","XPeng"), Map.entry("理想","Li Auto"),
        Map.entry("问界","AITO"), Map.entry("岚图","Voyah"), Map.entry("阿维塔","Avatr"),
        Map.entry("深蓝","Deepal"), Map.entry("埃安","Aion"), Map.entry("哪吒","Neta"),
        Map.entry("零跑","Leapmotor"), Map.entry("威马","Weltmeister"), Map.entry("高合","HiPhi"),
        Map.entry("智己","IM"), Map.entry("极狐","Arcfox"), Map.entry("腾势","Denza"),
        Map.entry("仰望","Yangwang"), Map.entry("方程豹","Fangchengbao"), Map.entry("捷途","Jetour"),
        Map.entry("星途","Exeed"), Map.entry("极氪","Zeekr"), Map.entry("领克","Lynk & Co"),
        Map.entry("坦克","Tank"), Map.entry("魏牌","WEY"), Map.entry("欧拉","Ora"),
        Map.entry("飞凡","Rising Auto"), Map.entry("吉普","Jeep"), Map.entry("菲亚特","Fiat"),
        Map.entry("铃木","Suzuki"), Map.entry("三菱","Mitsubishi"), Map.entry("五十铃","Isuzu"),
        Map.entry("依维柯","Iveco"), Map.entry("斯柯达","Skoda"), Map.entry("标致","Peugeot"),
        Map.entry("雪铁龙","Citroen"), Map.entry("现代","Hyundai"), Map.entry("起亚","Kia"),
        Map.entry("别克","Buick"), Map.entry("凯迪拉克","Cadillac"), Map.entry("林肯","Lincoln"),
        Map.entry("捷豹","Jaguar"), Map.entry("路虎","Land Rover"), Map.entry("宾利","Bentley"),
        Map.entry("劳斯莱斯","Rolls-Royce"), Map.entry("法拉利","Ferrari"), Map.entry("兰博基尼","Lamborghini"),
        Map.entry("玛莎拉蒂","Maserati"), Map.entry("阿斯顿马丁","Aston Martin"), Map.entry("迈凯伦","McLaren"),
        Map.entry("路特斯","Lotus"), Map.entry("阿尔法罗密欧","Alfa Romeo"), Map.entry("讴歌","Acura"),
        Map.entry("英菲尼迪","Infiniti"), Map.entry("斯巴鲁","Subaru"), Map.entry("中兴","ZX Auto"),
        Map.entry("东南","Soueast"), Map.entry("海马","Haima"), Map.entry("众泰","Zotye"),
        Map.entry("猎豹","Leopaard"), Map.entry("启辰","Venucia"), Map.entry("开瑞","Karry"),
        Map.entry("凯翼","Cowin"), Map.entry("长安铃木","Changan Suzuki"), Map.entry("东风本田","Dongfeng Honda"),
        Map.entry("东风日产","Dongfeng Nissan"), Map.entry("一汽丰田","FAW Toyota"), Map.entry("广汽丰田","GAC Toyota"),
        Map.entry("广汽本田","GAC Honda"), Map.entry("上汽大众","SAIC Volkswagen"), Map.entry("吉利汽车","Geely Auto"));
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
        // Split the text into pieces: non-Han runs pass through literally,
        // Han runs are scanned for brand names (see splitHanRun). Both
        // localize() passes go through here, so the sources collected in
        // pass one are exactly these split segments, never the whole run.
        List<String> pieces=new ArrayList<>();
        List<Boolean> brandPiece=new ArrayList<>();
        Matcher matcher=HAN.matcher(text);
        int pos=0;
        while(matcher.find()) {
            if(matcher.start()>pos) { pieces.add(text.substring(pos,matcher.start())); brandPiece.add(false); }
            splitHanRun(matcher.group(),translate,pieces,brandPiece);
            pos=matcher.end();
        }
        if(pos<text.length()) { pieces.add(text.substring(pos)); brandPiece.add(false); }
        StringBuilder out=new StringBuilder();
        boolean prevBrand=false;
        for(int i=0;i<pieces.size();i++) {
            String piece=pieces.get(i);
            if(piece.isEmpty()) continue;
            // A brand value jammed against a neighbouring Latin-letter
            // segment reads as one made-up word (WulingHongguang,
            // GeelyICON), so separate the seam with a space when both
            // sides of it are Latin letters.
            if(out.length()>0 && (brandPiece.get(i)||prevBrand)
                && isLatin(out.charAt(out.length()-1)) && isLatin(piece.charAt(0))) out.append(' ');
            out.append(piece);
            prevBrand=brandPiece.get(i);
        }
        return out.toString();
    }
    private static void splitHanRun(String run,UnaryOperator<String> translate,List<String> pieces,List<Boolean> brandPiece) {
        // Left to right: at each position the longest BRANDS key starting
        // there wins (长安铃木 beats 长安); the Han text accumulated between
        // two brand hits is handed to translate as a single segment, so
        // 五菱宏光 becomes Wuling + translate(宏光) instead of one machine
        // translation of the whole name.
        StringBuilder plain=new StringBuilder();
        int i=0;
        while(i<run.length()) {
            String brand=longestBrandAt(run,i);
            if(brand==null) { plain.append(run.charAt(i)); i++; continue; }
            if(plain.length()>0) { pieces.add(translate.apply(plain.toString())); brandPiece.add(false); plain.setLength(0); }
            pieces.add(BRANDS.get(brand)); brandPiece.add(true);
            i+=brand.length();
        }
        if(plain.length()>0) { pieces.add(translate.apply(plain.toString())); brandPiece.add(false); }
    }
    private static String longestBrandAt(String run,int offset) {
        String best=null;
        for(String key:BRANDS.keySet())
            if(run.startsWith(key,offset) && (best==null||key.length()>best.length())) best=key;
        return best;
    }
    private static boolean isLatin(char c) {
        return Character.isLetter(c) && Character.UnicodeScript.of(c)==Character.UnicodeScript.LATIN;
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
            List<String> outputs;
            boolean translated;
            try {
                outputs=translateBatch(batch,lang);
                translated=true;
            } catch(RuntimeException e) {
                // Translation is display-only sugar. If the translation
                // service is unreachable (e.g. LibreTranslate not installed
                // on this host), fall back to the source text instead of
                // failing the whole formula query with a 503 — visitors
                // still get the full colour list, just untranslated, and
                // nothing is cached so a later retry can translate for real.
                outputs=batch.stream().map(s->s.startsWith("en:")?s.substring(3):s).toList();
                translated=false;
            }
            for(int i=0;i<batch.size();i++) {
                String source=batch.get(i),text=outputs.get(i);
                if(translated) cachePut(source,lang,text);
                translations.put(source,text);
            }
        }
        return rewriteFields(tree,(field,text)->displayText(field,text,source->translations.getOrDefault(source,source)));
    }
    /**
     * Best-effort cache write. Deliberately avoids INSERT ... ON DUPLICATE
     * KEY UPDATE ... VALUES(col): with rewriteBatchedStatements=true the
     * MySQL driver miscounts that statement's parameters ("No value
     * specified for parameter 4") and the write explodes. A plain
     * update-then-insert has no such quirk, and any remaining failure is
     * swallowed — the cache must never fail the formula query itself.
     */
    private void cachePut(String source,String lang,String text) {
        try {
            int updated=jdbc.update("UPDATE eb_overseas_formula_translation SET translated_text=? WHERE source_hash=? AND lang=?",text,hash(source),lang);
            if(updated==0) jdbc.update("INSERT INTO eb_overseas_formula_translation(source_hash,lang,source_text,translated_text) VALUES(?,?,?,?)",hash(source),lang,source,text);
        } catch(RuntimeException ignored) {
            // Cache is an optimisation only; the response already has the text.
        }
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
