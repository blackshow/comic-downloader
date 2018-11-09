package me.blackshow.comic.downloader.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Data;

/**
 * Title : Catalog Author : blackshow Date : 2018-11-08
 */
@Data
@Entity
@Table(name = "Chapter", indexes = {@Index(name = "idx_catalog_id", columnList = "catalogId")})
public class Chapter implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(nullable = false, length = 20)
    private Long catalogId;

}
