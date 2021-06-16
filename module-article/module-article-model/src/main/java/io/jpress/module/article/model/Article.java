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
package io.jpress.module.article.model;

import io.jboot.db.annotation.Table;
import io.jboot.utils.StrUtil;
import io.jboot.web.controller.JbootControllerContext;
import io.jpress.JPressConsts;
import io.jpress.commons.utils.UrlUtils;
import io.jpress.commons.utils.CommonsUtils;
import io.jpress.commons.utils.JsoupUtils;
import io.jpress.commons.utils.MarkdownUtils;
import io.jpress.module.article.model.base.BaseArticle;
import io.jpress.web.seoping.PingData;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated by Jboot.
 */
@Table(tableName = "article", primaryKey = "id")
public class Article extends BaseArticle<Article> {

    public static final String STATUS_NORMAL = "normal";
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_TRASH = "trash";


    public boolean isNormal() {
        return STATUS_NORMAL.equals(getStatus());
    }

    public boolean isDraft() {
        return STATUS_DRAFT.equals(getStatus());
    }

    public boolean isTrash() {
        return STATUS_TRASH.equals(getStatus());
    }

    public String getHtmlView() {
        return StrUtil.isBlank(getStyle()) ? "article.html" : "article_" + getStyle().trim() + ".html";
    }

    /**
     * 用于渲染html模板，是否高亮
     *
     * @return
     */
    public boolean isActive() {
        Article currentArticle = JbootControllerContext.get().getAttr("article");

        //当前页面并不是文章详情页面
        if (currentArticle == null || currentArticle.getId() == null) {
            return false;
        }

        return currentArticle.getId().equals(getId());
    }

    /**
     * 保证模板可以通过 model.isActive 属性进行高亮判断，而非 model.isActive()
     *
     * @return
     */
    public boolean getIsActive() {
        return isActive();
    }

    public String getUrl() {
        String link = getLinkTo();
        if (StrUtil.isNotBlank(link)) {
            return link;
        }
        return UrlUtils.getUrl("/article/", StrUtil.isNotBlank(getSlug()) ? getSlug() : getId());
    }


    public String getUrlWithPageNumber(int pageNumber) {
        if (pageNumber <= 1) {
            return getUrl();
        }
        return UrlUtils.getUrl("/article/", StrUtil.isNotBlank(getSlug()) ? getSlug() : getId(), "-", pageNumber);
    }

    public boolean isCommentEnable() {
        Boolean cs = getCommentStatus();
        return cs != null && cs;
    }

    public String getText() {
        return JsoupUtils.getText(getContent());
    }

    @Override
    public String getContent() {
        String content = super.getContent();
        if (_isMarkdownMode()) {
            content = MarkdownUtils.toHtml(content);
            content = JsoupUtils.clean(content);
        }
        return content;
    }


    public boolean _isMarkdownMode() {
        return JPressConsts.EDIT_MODE_MARKDOWN.equals(getEditMode());
    }

    public String getOrignalContent() {
        return super.getContent();
    }


    /**
     * 获取文章的所有图片
     *
     * @return
     */
    public List<String> getImages() {
        return JsoupUtils.getImageSrcs(getContent());
    }

    /**
     * 获取前面几张图片
     *
     * @param count
     * @return
     */
    public List<String> getImages(int count) {
        List<String> list = getImages();
        if (list == null || list.size() <= count) {
            return list;
        }

        List<String> newList = new ArrayList<>();
        for (int i = 0; 0 < count; i++) {
            newList.add(list.get(i));
        }
        return newList;
    }

    public boolean hasImage() {
        return getFirstImage() != null;
    }

    public boolean hasVideo() {
        return getFirstVideo() != null;
    }

    public boolean hasAudio() {
        return getFirstAudio() != null;
    }

    public String getFirstImage() {
        return JsoupUtils.getFirstImageSrc(getContent());
    }

    public String getFirstVideo() {
        return JsoupUtils.getFirstVideoSrc(getContent());
    }

    public String getFirstAudio() {
        return JsoupUtils.getFirstAudioSrc(getContent());
    }

    public String getShowImage() {
        String thumbnail = getThumbnail();
        return StrUtil.isNotBlank(thumbnail) ? thumbnail : getFirstImage();
    }

    public String getHighlightContent() {
        String content =  getStr("highlightContent");
        return StrUtil.isNotBlank(content) ? content : CommonsUtils.maxLength(getText(),100,"...");
    }

    public void setHighlightContent(String highlightContent) {
        put("highlightContent", highlightContent);
    }

    public String getHighlightTitle() {
        String title =  getStr("highlightTitle");
        return StrUtil.isNotBlank(title) ? title : getTitle();
    }

    public void setHighlightTitle(String highlightTitle) {
        put("highlightTitle", highlightTitle);
    }

    @Override
    public boolean save() {
        CommonsUtils.escapeModel(this, "content", "summary");
        return super.save();
    }

    @Override
    public boolean update() {
        CommonsUtils.escapeModel(this, "content", "summary");
        return super.update();
    }

    public PingData toPingData() {
        return PingData.create(getTitle(), getUrl());
    }


}
