/**
 * Copyright (c) 2016-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.module.article.service.task;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.jfinal.aop.Aop;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.upload.UploadFile;
import io.jboot.components.schedule.annotation.Cron;
import io.jboot.components.schedule.annotation.FixedRate;
import io.jboot.utils.CollectionUtil;
import io.jboot.utils.FileUtil;
import io.jboot.utils.HttpUtil;
import io.jboot.utils.StrUtil;
import io.jpress.JPressOptions;
import io.jpress.commons.utils.AliyunOssUtils;
import io.jpress.commons.utils.AttachmentUtils;
import io.jpress.model.Attachment;
import io.jpress.module.article.model.Article;
import io.jpress.module.article.model.ArticleCategory;
import io.jpress.module.article.service.ArticleCategoryService;
import io.jpress.module.article.service.ArticleService;
import io.jpress.module.article.service.enums.CategoryEnums;
import io.jpress.module.article.service.search.ArticleSearcher;
import io.jpress.module.article.service.search.ArticleSearcherFactory;
import io.jpress.module.article.service.utils.HttpUrlUtil;
import io.jpress.service.AttachmentService;
import io.jpress.service.MenuService;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 * @Title: 用于更新文章的 访问 数量
 * @Package io.jpress.module.article.ta
 */
//@FixedRate(period = 60 * 6, initialDelay = 60)
@Cron(value = "10 15 * * *",daemon = true)
public class ArticleCreateFromWebTask implements Runnable {



    public static final String urlPath ="http://www.bluedimgs.com/wp-content/plugins/erphpdown/";
    public static final String loginUrl ="http://www.bluedimgs.com/wp-login.php";
    public static final String pageUrl ="http://www.bluedimgs.com/page/";
    public static final String DOWN = "http://www.bluedimgs.com/wp-content/plugins/erphpdown/";
    public static String cookie ="";
    public static final String P = "<h1>";
    public static final String L = "</fieldset>";
    public static final String O = "</h1>";
    public static final String C1 = "info_category info_ico";
    public static final String C2 = "info_date info_ico";
    public static final String C3 = "rel=\"category tag\">";
    public static final String A = "</a>";
    public static final String C4 = "<div id=\"post_content\">";
    public static final String C5 = "<a href=\"";
    public static final String C6 = "\" data-title=\"";
    public static final String C7 = "VIP免费）<a href=";
    public static final String C8 = " class='erphpdown-down";
    public static final String C9 = " class='erphpdown-down";
    public static final String C10 = "<!-- 以下内容不要动 -->";
    public static final String C11 = "<!-- 以上内容不要动 -->";
    public static final String C12 = "</span><a href='";
    public static final String C13 = "' target='";

    public static final String P1 = "<h2><a href=\"";
    public static final String P2 = "</a></h2>";
    public static final String P3 = "\" rel=\"bookmark\" target=\"_blank\" title=\"";
    public static final String P4 = "\">";

    public static final String B1 = "<p>";
    public static final String B2 = "</p>";
    public static final String B3 = "<p style=\"margin-left:0px;text-align:center;\">";
    public static final String B4 = "<img src=\"";
    public static final String B5 = "<blockquote class=\"wp-block-quote\">";
    public static final String B6 = "</blockquote>";


    private static final String ATTACHMENT_TARGET_PREFIX = "/attachment";



    @Inject
    private ArticleService articleService;
    @Inject
    private ArticleCategoryService categoryService;
    @Inject
    private AttachmentService service;


    @Override
    public void run() {
        this.doReptileStart();
    }



    /**
     * 定时任务爬取网站：http://www.bluedimgs.com
     * @throws IOException
     */

    public void doReptileStart() {


        try {

            //登录
            this.doLogin();
            //获取分页列表
            this.getPageList();
        }catch (Exception e){
            System.out.println("创建文章失败"+e);
        }
    }
    /**
     * 登录获取cookie
     * @throws IOException
     */
    public void doLogin() throws IOException {
//        JSONObject json = new JSONObject();
//        json.put("log","1677925467@qq.com");
//        json.put("pwd","blued925467");
//        json.put("wp-submit","登录");
//        json.put("redirect_to","http://www.bluedimgs.com/wp-admin/");
//        json.put("testcookie","1");
        String content = "log=1677925467%40qq.com&pwd=blued925467&wp-submit=%E7%99%BB%E5%BD%95&redirect_to=http%3A%2F%2Fwww.bluedimgs.com%2Fwp-admin%2F&testcookie=1";
        cookie = HttpUrlUtil.getCookieByLogin(content,loginUrl);
    }
    /**
     * 获取分页列表
     * @throws IOException
     */
    public void getPageList() throws IOException {
        Integer pageNum = 3;
        for (Integer i = 0; i < pageNum; i++) {
            String pagePath = pageUrl+i;
            String pageInfo = HttpUrlUtil.doGetForJson(null,pagePath,cookie);
            //21个链接，[0]个无效：http://www.bluedimgs.com/jerry-01.html" rel="bookmark" target="_blank" title="Jerry 01 哥的首本寫真">Jerry 01 哥的首本寫真
            List<String> pageList = Arrays.stream(pageInfo.split(P1)).map(t->t.split(P2)[0]).collect(Collectors.toList());
            pageList.remove(0);
            //详情链接<标题，链接>
            Map<String,String> articleUrlMap =pageList.stream().filter(t->t.contains(P3)).collect(Collectors.toMap(t->t.split(P3)[1].split(P4)[1], t->t.split(P3)[0]));
            System.out.println(articleUrlMap);

            //查询asiamales是否已存在，不存在才新建文章
            articleUrlMap.forEach((title,detailUrl)->{
                String keyWord = StrUtil.escapeHtml(title.trim());
                ArticleSearcher searcher = ArticleSearcherFactory.getSearcher();
                Page<Article> page = searcher.search(keyWord, pageNum, 20);

                //文章已存在
                if (page.getTotalRow() != 0) {
                    return ;
                }

                //查询详情页面，准备新建文章
                try {
                    this.getDetail(detailUrl);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        }
        System.out.println("刷数据完成-------------------------time="+new Date());
    }
    /**
     * 获取详情页
     * @throws IOException
     */
    public void getDetail(String detailUrl) throws IOException {
        String deatialPage = HttpUrlUtil.doGetForJson(null,detailUrl,cookie);
        //处理详情页
        this.dealDetailStr(deatialPage);
    }
    /**
     * 处理详情页
     * @param webDetailStr
     */
    public void dealDetailStr(String webDetailStr){
        if(!webDetailStr.contains("post_content")){
            System.out.println("查询不到文章详情");
            return;
        }
        //文章内容
        String comment = webDetailStr.split(P)[1].split(L)[0];

        //标题
        String title = comment.split(O)[0];
        //分类："><a href="http://www.bluedimgs.com/category/thai" rel="category tag">泰国帅哥</a></span>
        String category = comment.split(C1)[1].split(C2)[0];
        List<String> categoryList = Arrays.stream(category.split(C3)).map(t->t.split(A)[0]).collect(Collectors.toList());
        categoryList.remove(0);

        //文章切割
        String[] artArr = comment.split(C4)[1].split(C5);
        //图片，[0]为文章，其他为图片链接
        List<String> articleList = Arrays.stream(artArr).map(t->t.split(C6)[0]).collect(Collectors.toList());


        //文章
        String articleStr = articleList.get(0);
        //上传图片
        List<String> imgPathList = this.uploadForDownload(articleList);
        //打开下载页面的链接
        String downloadUrl = comment.split(C7)[1].split(C8)[0];

        //处理下载页面,得到下载块链接
        String downloadRealStr = dealDownloadUrl(downloadUrl);

        //构建文章
        String content = this.buildContent(articleStr,imgPathList,downloadRealStr);

        //新建文章
        this.createNewArticle(title,content,categoryList,imgPathList.get(0));
    }
    /**
     * 新建文章
     * @param title
     * @param content
     * @param categoryList
     * @param thumbnail
     */
    public void createNewArticle(String title,String content,List<String> categoryList,String thumbnail){
        Article article = new Article();
        article.setStatus(Article.STATUS_NORMAL);
        article.setUserId(Long.valueOf(1));
        article.setEditMode("html");
        article.setTitle(title);
        article.setContent(content);
        article.setSummary(title);
        article.setMetaKeywords(categoryList.stream().map(t->t).collect(Collectors.joining(",")));
        article.setMetaDescription(title);
        article.setOrderNumber(0);
        article.setCreated(new Date());
        article.setCommentStatus(true);
        article.setThumbnail(thumbnail);

        //保存文章
        this.doWriteSaveForBluedimgs(article,categoryList);

    }

    /**
     * 保存文章
     */
    public void doWriteSaveForBluedimgs(Article articleEntity,List<String> categoryList) {

        Article article = articleEntity;
        System.out.println("保存文章---------------"+article);

        //默认情况下，请求会被 escape，通过 getOriginalPara 获得非 escape 的数据
//        article.setContent(getCleanedOriginalPara(article.getContent()));


//        if (!validateSlug(article)) {
//            renderJson(Ret.fail("message", "固定连接不能以数字结尾"));
//            return;
//        }


        if (StrUtil.isNotBlank(article.getSlug())) {
            Article existArticle = articleService.findFirstBySlug(article.getSlug());
            if (existArticle != null && !existArticle.getId().equals(article.getId())) {
//                renderJson(Ret.fail("message", "该固定链接已经存在"));
                return;
            }
        }

        if (article.getCreated() == null){
            article.setCreated(new Date());
        }

        if (article.getModified() == null){
            article.setModified(new Date());
        }

        if (article.getOrderNumber() == null) {
            article.setOrderNumber(0);
        }

        long id = (long) articleService.saveOrUpdate(article);
        articleService.doUpdateCommentCount(id);

//        setAttr("articleId", id);
//        setAttr("article", article);


        Long[] saveBeforeCategoryIds = null;
        if (article.getId() != null){
            saveBeforeCategoryIds = categoryService.findCategoryIdsByArticleId(article.getId());
        }


//        Long[] categoryIds = getParaValuesToLong("category");
//        Long[] tagIds = getTagIds(getParaValues("tag"));
        Long[] categoryIds = ArrayUtils.toObject(Arrays.stream(CategoryEnums.values()).filter(t->categoryList.contains(t.getTitle())).mapToLong(t->t.getValue()).toArray());
        Long[] tagIds = getTagIds(ArrayUtils.toStringArray(categoryList.toArray()));

        Long[] updateCategoryIds = ArrayUtils.addAll(categoryIds, tagIds);

        articleService.doUpdateCategorys(id, updateCategoryIds);


        if (updateCategoryIds != null && updateCategoryIds.length > 0) {
            for (Long categoryId : updateCategoryIds) {
                categoryService.doUpdateArticleCount(categoryId);
            }
        }

        if (saveBeforeCategoryIds != null && saveBeforeCategoryIds.length > 0) {
            for (Long categoryId : saveBeforeCategoryIds) {
                categoryService.doUpdateArticleCount(categoryId);
            }
        }

        Ret ret = id > 0 ? Ret.ok().set("id", id) : Ret.fail();
//        renderJson(ret);
    }

    private Long[] getTagIds(String[] tags) {
        if (tags == null || tags.length == 0) {
            return null;
        }

        Set<String> tagset = new HashSet<>();
        for (String tag : tags) {
            tagset.addAll(StrUtil.splitToSet(tag,","));
        }

        List<ArticleCategory> categories = categoryService.doCreateOrFindByTagString(tagset.toArray(new String[0]));
        long[] ids = categories.stream().mapToLong(value -> value.getId()).toArray();
        return ArrayUtils.toObject(ids);
    }

    /**
     *
     * 构建文章
     * @param article
     * @param imgPathList
     * @param downloadRealStr
     * @return
     */
    public String buildContent(String articleStr,List<String> imgPathList,String downloadRealStr){

        String info = "<p>\n" +
                "    本资源单独售价10元，加入会员可以畅看全站所有资源。您可以【<a href=\"https://asiamales.com/article/7218\" target=\"_blank\"><strong>联系我们</strong></a>】购买。\n" +
                "</p>\n" +
                "<p>\n" +
                "    This resource is priced at 10 yuan separately. Joining members can enjoy all the resources on the site. You can [<a href=\"https://asiamales.com/article/7218\">contact us</a>] to buy.\n" +
                "</p>";

        String imgStr = "";
        if(!CollectionUtil.isEmpty(imgPathList)){
            imgStr = imgPathList.stream().map(t->B4+t+P4).collect(Collectors.joining(" "));
        }
        String content = B3+articleStr+B2+B1+imgStr+B2+B5+downloadRealStr+B6+info;
        System.out.println(content);
        return content;
    }
    /**
     * 上传图片
     * @param articleList
     */
    public List<String> uploadForDownload(List<String> articleList) {
        List<String> imgPathList = Lists.newArrayList();

        if(CollectionUtil.isEmpty(articleList)){
            return imgPathList;
        }
        List<String> imgUrlList = articleList;
        imgUrlList.remove(0);


        imgUrlList.forEach(imgUrl->{

            UploadFile uploadFile = HttpUrlUtil.doGetImg(imgUrl,cookie);
            if (uploadFile == null) {
//                renderJson(Ret.fail().set("message", "请选择要上传的文件"));
                return;
            }


            File file = uploadFile.getFile();
//            if (!getLoginedUser().isStatusOk()) {
//                file.delete();
//                renderJson(Ret.of("error", Ret.of("message", "当前用户未激活，不允许上传任何文件。")));
//                return;
//            }

            if (AttachmentUtils.isUnSafe(file)) {
                file.delete();
//                renderJson(Ret.fail().set("message", "不支持此类文件上传"));
                return;
            }

            String mineType = uploadFile.getContentType();
            String fileType = mineType.split("/")[0];

            int maxImgSize = JPressOptions.getAsInt("attachment_img_maxsize", 10);
            int maxOtherSize = JPressOptions.getAsInt("attachment_other_maxsize", 100);

            Integer maxSize = "image".equals(fileType) ? maxImgSize : maxOtherSize;

            int fileSize = Math.round(file.length() / 1024 * 100) / 100;
            if (maxSize > 0 && fileSize > maxSize * 1024) {
                file.delete();
//                renderJson(Ret.fail().set("message", "上传文件大小不能超过 " + maxSize + " MB"));
                return;
            }

            String path = AttachmentUtils.moveFile(uploadFile);
            AliyunOssUtils.upload(path, AttachmentUtils.file(path));

            Attachment attachment = new Attachment();
            attachment.setUserId(Long.valueOf(1));
            attachment.setTitle(uploadFile.getOriginalFileName());
            attachment.setPath(path.replace("\\", "/"));
            attachment.setSuffix(FileUtil.getSuffix(uploadFile.getFileName()));
            attachment.setMimeType(uploadFile.getContentType());

            Object attachmentId = service.save(attachment);

//            renderJson(Ret.ok().set("success", true)
//                    .set("src", attachment.getPath())
//                    .set("title",attachment.getTitle())
//                    .set("attachmentId",attachmentId)
//            );

            imgPathList.add(attachment.getPath());
        });
        return imgPathList;
    }
    /**
     * 处理下载链接获取真正下载地址
     * @param downloadUrl
     */
    public String dealDownloadUrl(String downloadUrl){
        //请求获取下载页面
        String downloadStr = HttpUrlUtil.doGetForJson(null,downloadUrl,cookie);
        //下载地址代码块
        String downloadRealStr = downloadStr.split(C10)[1].split(C11)[0];
        System.out.println("替换前======="+downloadRealStr);
        //(0)不是有效地址，请求后跳转的地址才是真正下载地址
        List<String> linkUrlList = Arrays.stream(downloadRealStr.split(C12)).filter(t->t.contains(C13)).map(t->t.split(C13)[0]).collect(Collectors.toList());

        //循环获取真正下载地址
        for (String link : linkUrlList) {
            String realUrl = this.getRealDownloadUrl(link);
            downloadRealStr = downloadRealStr.replace(link,realUrl);
        }
        downloadRealStr = downloadRealStr.replace("点击下载","【点击下载】");
        System.out.println("替换后======="+downloadRealStr);

        return downloadRealStr;
    }
    /**
     * 下载地址重定向后获得最终地址
     * @param args
     */
    public String getRealDownloadUrl(String downloadPhp) {


        StringBuffer buffer = new StringBuffer();
        try {
            String url = urlPath+downloadPhp;
            System.out.println("访问地址:" + url);

            //发送get请求
            URL serverUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) serverUrl.openConnection();
            conn.setRequestMethod("GET");
            //必须设置false，否则会自动redirect到重定向后的地址
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Cookie", cookie);
            conn.addRequestProperty("Accept-Charset", "UTF-8;");
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.8) Firefox/3.6.8");
//            conn.addRequestProperty("Referer", "http://matols.com/");
            conn.connect();

            //判定是否会进行302重定向
            if (conn.getResponseCode() == 302) {
                //如果会重定向，保存302重定向地址，以及Cookies,然后重新发送请求(模拟请求)
                String location = conn.getHeaderField("Location");
                String cookies = conn.getHeaderField("Set-Cookie");
                serverUrl = new URL(urlPath+location);
                conn = (HttpURLConnection) serverUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Cookie", cookie);
                conn.addRequestProperty("Accept-Charset", "UTF-8;");
                conn.addRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.8) Firefox/3.6.8");
//                conn.addRequestProperty("Referer", "http://matols.com/");
                conn.connect();
                System.out.println("跳转地址:" + urlPath+location);

            }

            //将返回的输入流转换成字符串
            InputStream inputStream = conn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"utf-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            System.out.println(buffer.toString());
            bufferedReader.close();

            inputStreamReader.close();
            // 释放资源
            inputStream.close();
            inputStream = null;




        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("返回真正地址="+buffer.toString());
        return buffer.toString().split("location='")[1].split("';")[0];
    }
}
