/*
    Polypus: a Big Data Self-Deployable Architecture for Microblogging 
    Text Extraction and Real-Time Sentiment Analysis

    Copyright (C) 2017 Rodrigo Martínez (brunneis) <dev@brunneis.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.brunneis.polypus.polypus4t.vo;

/**
 * Esta clase se corresponde con el contenido almacenado en HBase
 *
 * @author brunneis
 */
public class DigitalPost {

    private String source;
    private String id;
    private String language;
    private String content;
    private String authorName;
    private String authorNickname;
    private String authorId;
    private String postId;
    private Long publicationTimestamp;
    private Integer sentiment;

    public DigitalPost(
            String source,
            String language,
            String content,
            String authorName,
            String authorNickname,
            String authorId,
            String postId,
            String publicationTimestamp,
            Double relevance
    ) {
        this.source = source;
        this.language = language;
        this.content = content;
        this.authorName = authorName;
        this.authorNickname = authorNickname;
        this.authorId = authorId;
        this.postId = postId;
        this.publicationTimestamp = Long.parseLong(publicationTimestamp);
    }

    public DigitalPost(DigitalPost dp) {
        this.source = dp.getSource();
        this.language = dp.getLanguage();
        this.content = dp.getContent();
        this.authorName = dp.getAuthorName();
        this.authorNickname = dp.getAuthorNickname();
        this.authorId = dp.getAuthorId();
        this.postId = dp.getPostId();
        this.publicationTimestamp = dp.getPublicationTimestamp();
        this.sentiment = dp.getSentiment();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public Long getPublicationTimestamp() {
        return publicationTimestamp;
    }

    public void setPublicationTimestamp(Long publicationTimestamp) {
        this.publicationTimestamp = publicationTimestamp;
    }

    public Integer getSentiment() {
        return sentiment;
    }

    public void setSentiment(Integer sentiment) {
        this.sentiment = sentiment;
    }

}
